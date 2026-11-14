package engine.execution;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Operations startRun, getLatestRun, getRun, listRunEvents, streamRunEvents and downloadRunZip. */
@RestController
class RunController {

    private final RunService service;

    RunController(RunService service) {
        this.service = service;
    }

    @PostMapping("/api/projects/{id}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    RunView start(@PathVariable UUID id) {
        return service.start(id);
    }

    @GetMapping("/api/projects/{id}/runs/latest")
    RunView latest(@PathVariable UUID id) {
        return service.latest(id);
    }

    @GetMapping("/api/runs/{runId}")
    RunView get(@PathVariable UUID runId) {
        return service.view(runId);
    }

    @GetMapping("/api/runs/{runId}/events")
    List<RunEvent> events(@PathVariable UUID runId, @RequestParam(defaultValue = "0") long after) {
        return service.events(runId, after);
    }

    @GetMapping(value = "/api/runs/{runId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@PathVariable UUID runId, @RequestParam(required = false) Long after,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
        long from = after != null ? after : parse(lastEventId);
        return service.stream(runId, from);
    }

    @GetMapping("/api/runs/{runId}/zip")
    ResponseEntity<StreamingResponseBody> zip(@PathVariable UUID runId) {
        Workspace workspace = service.workspaceForZip(runId);
        StreamingResponseBody body = workspace::zip;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("run-" + runId.toString().substring(0, 8) + ".zip").build().toString())
                .body(body);
    }

    private static long parse(String lastEventId) {
        try {
            return lastEventId == null ? 0 : Long.parseLong(lastEventId.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
