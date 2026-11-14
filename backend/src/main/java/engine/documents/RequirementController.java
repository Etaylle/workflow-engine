package engine.documents;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operations generateRequirements and getRequirements of the API. */
@RestController
class RequirementController {

    private final RequirementService service;

    RequirementController(RequirementService service) {
        this.service = service;
    }

    @PostMapping("/api/projects/{id}/requirements")
    RequirementsView generate(@PathVariable UUID id) {
        return service.generate(id);
    }

    @GetMapping("/api/projects/{id}/requirements")
    RequirementsView get(@PathVariable UUID id) {
        return service.find(id);
    }
}
