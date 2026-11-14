package engine.projects;

import engine.shared.ApiProblem;
import engine.shared.SessionKey;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operations createProject, listProjects and getProject of api/openapi.yaml. */
@RestController
public class ProjectController {

    /** Request body; the key never appears in toString. */
    public record CreateProjectRequest(String wish, String apiKey) {
        @Override
        public String toString() {
            return "CreateProjectRequest[wish=" + wish + ", apiKey=***]";
        }
    }

    private final ProjectService projects;
    private final SessionKey sessionKey;

    public ProjectController(ProjectService projects, SessionKey sessionKey) {
        this.projects = projects;
        this.sessionKey = sessionKey;
    }

    @PostMapping("/api/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@RequestBody CreateProjectRequest request) {
        Project project = projects.create(request.wish());
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            sessionKey.set(request.apiKey().strip());
        }
        return project;
    }

    @GetMapping("/api/projects")
    public List<Project> list() {
        return projects.list();
    }

    @GetMapping("/api/projects/{id}")
    public Project get(@PathVariable UUID id) {
        return projects
                .get(id)
                .orElseThrow(() -> new ApiProblem(404, "Project not found", "No project " + id));
    }
}
