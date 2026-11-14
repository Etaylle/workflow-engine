package engine.workflows;

import engine.shared.ApiProblem;
import engine.shared.ProjectPort;
import engine.shared.SessionKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Writes, validates, stores and confirms the workflow of a project. */
@Service
class WorkflowService {

    private final WorkflowWriter writer;
    private final WorkflowStore store;
    private final ProjectPort projects;
    private final SessionKey sessionKey;
    private final JsonMapper json;

    WorkflowService(
            WorkflowWriter writer,
            WorkflowStore store,
            ProjectPort projects,
            SessionKey sessionKey,
            JsonMapper json) {
        this.writer = writer;
        this.store = store;
        this.projects = projects;
        this.sessionKey = sessionKey;
        this.json = json;
    }

    /** One planning call; one retry with the reason when the answer is invalid. */
    WorkflowView generate(UUID projectId) {
        String wish = projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        String key = sessionKey.get().orElseThrow(() -> new ApiProblem(409, "Anthropic key missing",
                "No Anthropic key is set for this session. Enter your key and try again."));
        WorkflowDraft draft = writeValid(key, wish);
        WorkflowView view = store.replace(projectId, draft);
        projects.markStatus(projectId, "workflow_drafted");
        return view;
    }

    private WorkflowDraft writeValid(String key, String wish) {
        String correction = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                WorkflowDraft draft = writer.write(key, wish, correction);
                WorkflowValidator.validate(draft);
                return draft;
            } catch (InvalidDraftException e) {
                if (attempt == 2) {
                    throw new ApiProblem(502, "Invalid workflow from Claude",
                            "Claude's workflow was invalid twice (" + e.getMessage()
                                    + "). Try again or reword the wish.", e);
                }
                correction = e.getMessage();
            }
        }
        throw new IllegalStateException("unreachable");
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
        String snapshot = json.writeValueAsString(new WorkflowStore.Snapshot(wish, view));
        ContractView contract = store.confirm(projectId, snapshot).orElseThrow(() -> new ApiProblem(
                409, "Nothing to confirm", "The workflow changed while confirming; try again."));
        projects.markStatus(projectId, "confirmed");
        return contract;
    }

    List<ContractView> contracts(UUID projectId) {
        projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        return store.contracts(projectId);
    }

    private static ApiProblem notFound(UUID id) {
        return new ApiProblem(404, "Project not found", "No project " + id);
    }
}
