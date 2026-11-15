package engine.execution;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operations getBuilders and setClaudeCodeAuth of api/openapi.yaml: what a build needs, per builder. */
@RestController
class BuilderController {

    /** Every builder with what it has and lacks; demo is true when a stub builds everything. */
    record BuildersView(boolean demo, List<BuilderState> builders) {
    }

    record AuthRequest(String mode) {
    }

    private final Builders builders;
    private final ClaudeAuth auth;
    private final ToolProbe probe;

    BuilderController(Builders builders, ClaudeAuth auth, ToolProbe probe) {
        this.builders = builders;
        this.auth = auth;
        this.probe = probe;
    }

    @GetMapping("/api/builders")
    BuildersView get(@RequestParam(defaultValue = "false") boolean refresh) {
        if (refresh) {
            probe.forget();
        }
        return new BuildersView(builders.demo(), builders.states());
    }

    /** Chooses how Claude Code authenticates (kept in memory with the keys) and answers the new state. */
    @PutMapping("/api/builders/claude_code/auth")
    BuildersView setClaudeAuth(@RequestBody AuthRequest request) {
        auth.choose(request.mode());
        probe.forget();
        return get(false);
    }
}
