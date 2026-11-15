package engine.status;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operation getStatus of api/openapi.yaml. */
@RestController
public class StatusController {

    private final String name;
    private final String buildId;
    private final Clock clock;
    private final boolean aiStub;

    public StatusController(
            @Value("${spring.application.name}") String name,
            BuildProperties build,
            Clock clock,
            @Value("${engine.ai.stub:false}") boolean aiStub) {
        this.name = name;
        this.buildId = build.getVersion();
        this.clock = clock;
        this.aiStub = aiStub;
    }

    @GetMapping("/api/status")
    public EngineStatus status() {
        return new EngineStatus(name, buildId, clock.instant(), aiStub);
    }
}
