package engine.execution;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic builder for tests and ENGINE_AI_STUB=true: writes one small file per step. A wish
 * (and so CONTRACT.md) may hold markers that make it misbehave, so one run can be shown with a
 * defect: "stub-fail:build:3" makes the first 3 invocations of step build fail, "stub-slow:500"
 * makes every step take 500 ms, "stub-crash:build:1" makes the first invocation of step build
 * throw (the orchestrator turns that into an interrupted run, as an engine crash would),
 * "stub-limit:build:2" makes the first 2 invocations answer "rate limit reached" (the run waits
 * and tries again).
 */
class StubBuilder implements Builder {

    private static final Pattern FAIL = Pattern.compile("stub-fail:(\\w+):(\\d+)");
    private static final Pattern CRASH = Pattern.compile("stub-crash:(\\w+):(\\d+)");
    private static final Pattern LIMIT = Pattern.compile("stub-limit:(\\w+):(\\d+)");
    private static final Pattern SLOW = Pattern.compile("stub-slow:(\\d+)");

    private final Map<String, Integer> invocations = new ConcurrentHashMap<>();

    @Override
    public void checkReady() {
        // The stub needs nothing.
    }

    @Override
    public StepOutcome run(StepRequest request) {
        String contract = read(request.workspace().resolve("CONTRACT.md"));
        sleep(SLOW.matcher(contract));
        if (triggered(CRASH, contract, request, "crash")) {
            throw new IllegalStateException("Stub: step " + request.stepKey() + " crashed on purpose");
        }
        if (triggered(LIMIT, contract, request, "limit")) {
            return new StepOutcome(false, BigDecimal.ZERO, 0, 0, "rate_limit",
                    "Stub: rate limit reached");
        }
        if (triggered(FAIL, contract, request, "fail")) {
            return new StepOutcome(false, new BigDecimal("0.01"), 1, 0, "error_stub",
                    "Stub: step " + request.stepKey() + " failed on purpose");
        }
        request.console().accept("runs ./check.sh (stub)");
        try {
            Path docs = Files.createDirectories(request.workspace().resolve("docs"));
            Files.writeString(docs.resolve("STUB-" + request.stepKey() + ".md"),
                    "Step " + request.stepKey() + " written by the stub builder.\n");
            request.console().accept("writes docs/STUB-" + request.stepKey() + ".md");
        } catch (IOException e) {
            throw new UncheckedIOException("The stub builder could not write its file", e);
        }
        return new StepOutcome(true, new BigDecimal("0.01"), 1, 0, "success",
                "Stub: step " + request.stepKey() + " done");
    }

    /** True when a marker names this step and its count of invocations is not used up yet. */
    private boolean triggered(Pattern marker, String contract, StepRequest request, String what) {
        Matcher match = marker.matcher(contract);
        while (match.find()) {
            String key = what + request.workspace() + ":" + request.stepKey();
            if (match.group(1).equals(request.stepKey())
                    && invocations.merge(key, 1, Integer::sum) <= Integer.parseInt(match.group(2))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir,
            java.util.function.Consumer<String> console) {
        console.accept("check: CHECK PASSED (stub)");
        return new GateResult(true, List.of("CHECK PASSED (stub)"));
    }

    private static String read(Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (IOException e) {
            throw new UncheckedIOException("The stub builder could not read " + file, e);
        }
    }

    private static void sleep(Matcher slow) {
        if (!slow.find()) {
            return;
        }
        try {
            Thread.sleep(Long.parseLong(slow.group(1)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
