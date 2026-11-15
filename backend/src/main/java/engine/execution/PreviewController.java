package engine.execution;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operations getRunPreview, startRunPreview and stopRunPreview. */
@RestController
class PreviewController {

    private final RunPreviews previews;
    private final RunService runs;

    PreviewController(RunPreviews previews, RunService runs) {
        this.previews = previews;
        this.runs = runs;
    }

    @GetMapping("/api/runs/{runId}/preview")
    PreviewView get(@PathVariable UUID runId) {
        runs.view(runId);
        return previews.view(runId);
    }

    @PostMapping("/api/runs/{runId}/preview")
    @ResponseStatus(HttpStatus.ACCEPTED)
    PreviewView start(@PathVariable UUID runId) {
        runs.view(runId);
        return previews.start(runId);
    }

    @DeleteMapping("/api/runs/{runId}/preview")
    PreviewView stop(@PathVariable UUID runId) {
        runs.view(runId);
        return previews.stop(runId);
    }
}
