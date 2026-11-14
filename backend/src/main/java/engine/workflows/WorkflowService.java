package engine.workflows;


import engine.shared.AnswersPort;
import engine.shared.ApiProblem;
import engine.shared.ClaudeCall;
import engine.shared.InvalidAiOutputException;
import engine.shared.ProjectPort;
import engine.shared.RequirementsPort;
import engine.shared.SessionKey;
import engine.shared.WorkflowPort;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Writes, validates, stores and confirms the workflow of a project. */
@Service
class WorkflowService implements WorkflowPort {

    private final WorkflowWriter writer;
    private final WorkflowStore store;
    private final ProjectPort projects;
    private final AnswersPort answers;
    private final RequirementsPort requirements;
    private final SessionKey sessionKey;
    private final JsonMapper json;

    WorkflowService(WorkflowWriter writer, WorkflowStore store, ProjectPort projects,
            AnswersPort answers, RequirementsPort requirements, SessionKey sessionKey, JsonMapper json) {
        this.writer = writer;
        this.store = store;
        this.projects = projects;
        this.answers = answers;
        this.requirements = requirements;
        this.sessionKey = sessionKey;
        this.json = json;
    }

    /** One planning call from the planning material; one retry with the reason when invalid. */
    WorkflowView generate(UUID projectId) {
        String wish = projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        String status = projects.statusOf(projectId).orElseThrow(() -> notFound(projectId));
        if (!Set.of("requirements_ready", "workflow_drafted", "confirmed").contains(status)) {
            throw new ApiProblem(409, "Requirements missing", "The workflow is drawn from the"
                    + " requirements; answer the questions and write the requirements first.");
        }
        String key = ClaudeCall.requireKey(sessionKey);
        String material = material(projectId);
        WorkflowDraft draft = ClaudeCall.validated("workflow",
                correction -> writer.write(key, wish, material, correction), WorkflowValidator::validate);
        WorkflowView view = store.replace(projectId, draft);
        projects.markStatus(projectId, "workflow_drafted");
        return view;
    }

    /**
     * Replaces the workflow by one the client edited. The rules are the same as for Claude's draft;
     * a confirmed workflow becomes valid again, so confirming makes the next contract version.
     */
    WorkflowView edit(UUID projectId, WorkflowDraft draft) {
        String status = projects.statusOf(projectId).orElseThrow(() -> notFound(projectId));
        if (!Set.of("workflow_drafted", "confirmed").contains(status)) {
            throw new ApiProblem(409, "No workflow to edit", "Generate the workflow first.");
        }
        try {
            WorkflowValidator.validate(draft);
        } catch (InvalidAiOutputException e) {
            throw new ApiProblem(400, "Workflow invalid", e.getMessage());
        }
        WorkflowView view = store.replace(projectId, draft);
        projects.markStatus(projectId, "workflow_drafted");
        return view;
    }

    private String material(UUID projectId) {
        StringBuilder text = new StringBuilder("Brief:\n")
                .append(projects.briefOf(projectId).orElse("(none)")).append("\n\nAnswers of the client:");
        answers.answersOf(projectId).forEach(
                line -> text.append("\n- ").append(line.question()).append(" -> ").append(line.answer()));
        text.append("\n\nRequirements:");
        requirements.requirementsOf(projectId).forEach(r -> text.append("\n- ").append(r.key())
                .append(" (").append(r.priority()).append(", ").append(r.epic()).append(") ")
                .append(r.text()));
        return text.toString();
    }

    Optional<WorkflowView> find(UUID projectId) {
        projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        return store.find(projectId);
    }

    ContractView confirm(UUID projectId) {
        String wish = projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        WorkflowView view = store.find(projectId).orElseThrow(() -> new ApiProblem(409,
                "No workflow", "The project has no workflow yet. Generate it first."));
        if (!"valid".equals(view.status())) {
            throw new ApiProblem(409, "Nothing to confirm",
                    "The workflow is already confirmed and has not changed since.");
        }
        String snapshot = json.writeValueAsString(new WorkflowStore.Snapshot(wish,
                projects.briefOf(projectId).orElse(null), answers.answersOf(projectId),
                requirements.requirementsOf(projectId), view));
        ContractView contract = store.confirm(projectId, snapshot).orElseThrow(() -> new ApiProblem(
                409, "Nothing to confirm", "The workflow changed while confirming; try again."));
        projects.markStatus(projectId, "confirmed");
        return contract;
    }

    List<ContractView> contracts(UUID projectId) {
        projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        return store.contracts(projectId);
    }

    @Override
    public List<StepLine> stepsOf(UUID projectId) {
        return store.find(projectId).map(view -> {
            java.util.Map<UUID, String> lanes = new java.util.HashMap<>();
            view.lanes().forEach(lane -> lanes.put(lane.id(), lane.name()));
            return view.nodes().stream()
                    .map(n -> new StepLine(lanes.get(n.laneId()), n.kind(), n.title(), n.description(),
                            n.taskType()))
                    .toList();
        }).orElse(List.of());
    }

    private static ApiProblem notFound(UUID id) {
        return new ApiProblem(404, "Project not found", "No project " + id);
    }
}
