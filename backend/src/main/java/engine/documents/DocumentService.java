package engine.documents;

import engine.shared.AnswersPort;
import engine.shared.ApiProblem;
import engine.shared.ProjectPort;
import engine.shared.RequirementsPort;
import engine.shared.WorkflowPort;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Builds the requirements document and the project plan from what the project holds now. */
@Service
class DocumentService {

    private final ProjectPort projects;
    private final AnswersPort answers;
    private final RequirementsPort requirements;
    private final WorkflowPort workflows;

    DocumentService(ProjectPort projects, AnswersPort answers, RequirementsPort requirements,
            WorkflowPort workflows) {
        this.projects = projects;
        this.answers = answers;
        this.requirements = requirements;
        this.workflows = workflows;
    }

    /** The document of the kind "requirements" or "plan"; 404 for another kind, 409 when too early. */
    DocumentModel build(UUID projectId, String kind) {
        String name = projects.nameOf(projectId).orElseThrow(
                () -> new ApiProblem(404, "Project not found", "No project " + projectId));
        var material = new DocumentBuilder.Material(name, projects.wishOf(projectId).orElse(""),
                projects.briefOf(projectId).orElse(null), answers.answersOf(projectId),
                requirements.requirementsOf(projectId), workflows.stepsOf(projectId));
        return switch (kind) {
            case "requirements" -> {
                require(!material.requirements().isEmpty(), "Write the requirements first.");
                yield DocumentBuilder.requirements(material);
            }
            case "plan" -> {
                require(!material.steps().isEmpty(), "Write the workflow first; the plan follows it.");
                yield DocumentBuilder.plan(material);
            }
            default -> throw new ApiProblem(404, "Document not found",
                    "Documents are 'requirements' and 'plan', not '" + kind + "'.");
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new ApiProblem(409, "Document not ready", message);
        }
    }
}
