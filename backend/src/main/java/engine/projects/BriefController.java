package engine.projects;

import engine.shared.ApiProblem;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operations generateBrief and getBrief of the API. */
@RestController
class BriefController {

    private final BriefService service;

    BriefController(BriefService service) {
        this.service = service;
    }

    @PostMapping("/api/projects/{id}/brief")
    Brief generate(@PathVariable UUID id) {
        return service.generate(id);
    }

    @GetMapping("/api/projects/{id}/brief")
    Brief get(@PathVariable UUID id) {
        return service.find(id).orElseThrow(
                () -> new ApiProblem(404, "No brief", "The project has no brief yet."));
    }
}
