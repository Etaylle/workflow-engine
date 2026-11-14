package engine.workflows;

import engine.shared.ApiProblem;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operations generateWorkflow, getWorkflow, confirmWorkflow and listContracts of the API. */
@RestController
class WorkflowController {

    private final WorkflowService service;

    WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @PostMapping("/api/projects/{id}/workflow")
    WorkflowView generate(@PathVariable UUID id) {
        return service.generate(id);
    }

    @GetMapping("/api/projects/{id}/workflow")
    WorkflowView get(@PathVariable UUID id) {
        return service.find(id).orElseThrow(
                () -> new ApiProblem(404, "No workflow", "The project has no workflow yet."));
    }

    @PostMapping("/api/projects/{id}/workflow/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    ContractView confirm(@PathVariable UUID id) {
        return service.confirm(id);
    }

    @GetMapping("/api/projects/{id}/contracts")
    List<ContractView> contracts(@PathVariable UUID id) {
        return service.contracts(id);
    }
}
