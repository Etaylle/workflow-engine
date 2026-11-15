package engine.execution;

import engine.shared.ApiProblem;
import engine.shared.SessionKey;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Claude Code headless, one invocation per step, as measured in the build spike (D-M3-1).
 * The user's key goes to the child only as the environment variable ANTHROPIC_API_KEY.
 */
class ClaudeCodeBuilder implements Builder {

    private static final String PREAMBLE = "You are one step of a task build run by an orchestrator,"
            + " working alone in the current folder (the workspace). Read CLAUDE.md and CONTRACT.md"
            + " first. Do only the step below, nothing from other steps. Earlier steps left their"
            + " documents in docs/. Do not run git commit or any other git command that changes the"
            + " repository; the orchestrator commits. Use ./check.sh as the only way to build and"
            + " test everything; it prints CHECK PASSED when green. Finish with one line saying"
            + " what you did.";

    private static final String ALLOWED = "Read,Edit,Write,Glob,Grep,Bash(./check.sh:*),"
            + "Bash(./gradlew:*),Bash(npm:*),Bash(cd:*),Bash(git status:*),Bash(git diff:*),"
            + "Bash(curl:*),Bash(ls:*),Bash(cat:*),Bash(mkdir:*),Bash(sleep:*),Bash(kill:*)";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SessionKey sessionKey;
    private final ExecutionSettings settings;

    ClaudeCodeBuilder(SessionKey sessionKey, ExecutionSettings settings) {
        this.sessionKey = sessionKey;
        this.settings = settings;
    }

    @Override
    public void checkReady() {
        if (sessionKey.get().isEmpty() && !settings.useSignIn()) {
            throw new ApiProblem(409, "No Anthropic key",
                    "The build needs your Anthropic key. Enter it in the new project form or the AI"
                            + " connection section; the engine keeps it in memory only.");
        }
    }

    @Override
    public StepOutcome run(StepRequest request) {
        Path out = request.logDir().resolve(request.stepKey() + ".json");
        Path err = request.logDir().resolve(request.stepKey() + ".err");
        long start = System.nanoTime();
        List<String> command = List.of(settings.claudeBin(), "-p", PREAMBLE + "\n\n" + request.prompt(),
                "--model", settings.model(), "--output-format", "json", "--permission-mode",
                "acceptEdits", "--max-budget-usd", request.capUsd().toPlainString(),
                "--no-session-persistence", "--setting-sources", "project,local",
                "--strict-mcp-config", "--disable-slash-commands", "--tools",
                "Read,Edit,Write,Glob,Grep,Bash", "--allowedTools", ALLOWED);
        ProcessRunner.Result result = ProcessRunner.run(command, request.workspace(), childEnv(request),
                out, err, settings.stepTimeout(), request.cancelled());
        int seconds = (int) ((System.nanoTime() - start) / 1_000_000_000L);
        return outcome(result, out, err, seconds);
    }

    private Map<String, String> childEnv(StepRequest request) {
        Map<String, String> env = new HashMap<>(request.appEnv());
        if (!settings.useSignIn()) {
            sessionKey.get().ifPresent(key -> env.put("ANTHROPIC_API_KEY", key));
        }
        return env;
    }

    private StepOutcome outcome(ProcessRunner.Result result, Path out, Path err, int seconds) {
        String raw = read(out) + read(err);
        if (raw.contains("HARD STOP")) {
            return failed(seconds, "hard_stop", "Claude Code refused: HARD STOP (usage limit reached)");
        }
        if (result.cancelled()) {
            return failed(seconds, "cancelled", "The step was ended because the client stopped the run");
        }
        if (result.timedOut()) {
            return failed(seconds, "timeout", "The step took longer than " + settings.stepTimeout()
                    .toMinutes() + " minutes and was stopped");
        }
        try {
            JsonNode json = JSON.readTree(read(out));
            String subtype = json.path("subtype").asString("unknown");
            boolean ok = result.exitCode() == 0 && !json.path("is_error").asBoolean(false)
                    && "success".equals(subtype);
            String limit = ok ? null : LimitDetector.classify(json.path("result").asString("")
                    + read(err));
            return new StepOutcome(ok, BigDecimal.valueOf(json.path("total_cost_usd").asDouble(0)),
                    json.path("num_turns").asInt(0), seconds, limit != null ? limit : subtype,
                    oneLine(json.path("result").asString("")));
        } catch (RuntimeException e) {
            String limit = LimitDetector.classify(read(err) + read(out));
            return failed(seconds, limit != null ? limit : "no_json",
                    "Claude Code gave no readable answer (exit code " + result.exitCode() + "); see "
                            + err.getFileName());
        }
    }

    private static StepOutcome failed(int seconds, String subtype, String summary) {
        return new StepOutcome(false, BigDecimal.ZERO, 0, seconds, subtype, summary);
    }

    @Override
    public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir) {
        Path out = logDir.resolve("check.log");
        ProcessRunner.Result result = ProcessRunner.run(List.of("./check.sh"), workspace, appEnv, out,
                logDir.resolve("check.err"), settings.checkTimeout());
        List<String> lines = new ArrayList<>(List.of((read(out) + read(logDir.resolve("check.err"))).split("\n")));
        boolean passed = result.exitCode() == 0 && lines.stream().anyMatch("CHECK PASSED"::equals);
        if (result.timedOut()) {
            lines.add("The check took longer than " + settings.checkTimeout().toMinutes()
                    + " minutes and was stopped");
        }
        return new GateResult(passed, lines.subList(Math.max(0, lines.size() - 12), lines.size()));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String oneLine(String text) {
        String line = text.strip().replace('\n', ' ');
        return line.length() > 300 ? line.substring(0, 300) + "..." : line;
    }
}
