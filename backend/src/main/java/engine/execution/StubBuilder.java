package engine.execution;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Deterministic builder for tests and ENGINE_AI_STUB=true: writes one small file per step, never fails. */
class StubBuilder implements Builder {

    @Override
    public void checkReady() {
        // The stub needs nothing.
    }

    @Override
    public StepOutcome run(StepRequest request) {
        try {
            Path docs = Files.createDirectories(request.workspace().resolve("docs"));
            Files.writeString(docs.resolve("STUB-" + request.stepKey() + ".md"),
                    "Step " + request.stepKey() + " written by the stub builder.\n");
        } catch (IOException e) {
            throw new UncheckedIOException("The stub builder could not write its file", e);
        }
        return new StepOutcome(true, new BigDecimal("0.01"), 1, 0, "success",
                "Stub: step " + request.stepKey() + " done");
    }

    @Override
    public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir) {
        return new GateResult(true, List.of("CHECK PASSED (stub)"));
    }
}
