package engine.documents;

import engine.shared.AnswersPort;
import engine.shared.ApiProblem;
import engine.shared.AiTarget;
import engine.shared.PlanningCall;
import engine.shared.ProjectPort;
import engine.shared.RequirementsPort;
import engine.shared.SessionKey;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Writes (one planning call, one retry), stores and reads the requirements of a project. */
@Service
class RequirementService implements RequirementsPort {

    private final RequirementWriter writer;
    private final RequirementStore store;
    private final ProjectPort projects;
    private final AnswersPort answers;
    private final SessionKey sessionKey;

    RequirementService(RequirementWriter writer, RequirementStore store, ProjectPort projects,
            AnswersPort answers, SessionKey sessionKey) {
        this.writer = writer;
        this.store = store;
        this.projects = projects;
        this.answers = answers;
        this.sessionKey = sessionKey;
    }

    /** Writes the requirements from the wish and the answers; replaces earlier ones. */
    RequirementsView generate(UUID projectId) {
        String wish = projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        String status = projects.statusOf(projectId).orElseThrow(() -> notFound(projectId));
        if (!Set.of("answered", "requirements_ready").contains(status)) {
            throw new ApiProblem(409, "Requirements not possible", "Requirements are written from"
                    + " answered questions, and only until the workflow is drawn from them.");
        }
        AiTarget target = PlanningCall.requireTarget(sessionKey);
        projects.recordAi(projectId, target);
        String material = material(projectId);
        RequirementDraft draft = PlanningCall.validated("requirements",
                correction -> writer.write(target, wish, material, correction), RequirementValidator::validate);
        store.replace(projectId, draft);
        projects.markStatus(projectId, "requirements_ready");
        return store.find(projectId);
    }

    private String material(UUID projectId) {
        StringBuilder text = new StringBuilder("Brief:\n")
                .append(projects.briefOf(projectId).orElse("(none)")).append("\n\nAnswers of the client:");
        List<AnswersPort.AnswerLine> lines = answers.answersOf(projectId);
        for (AnswersPort.AnswerLine line : lines) {
            text.append("\n- ").append(line.question()).append(" -> ").append(line.answer());
        }
        return text.toString();
    }

    RequirementsView find(UUID projectId) {
        projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        return store.find(projectId);
    }

    @Override
    public List<RequirementLine> requirementsOf(UUID projectId) {
        return store.lines(projectId);
    }

    private static ApiProblem notFound(UUID id) {
        return new ApiProblem(404, "Project not found", "No project " + id);
    }
}
