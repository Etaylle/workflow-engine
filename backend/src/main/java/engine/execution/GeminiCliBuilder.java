package engine.execution;

import static engine.execution.BuilderSupport.failed;
import static engine.execution.BuilderSupport.oneLine;
import static engine.execution.BuilderSupport.read;

import engine.shared.Harness;
import engine.shared.SessionKey;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gemini CLI headless (D-77), one invocation per step: {@code gemini -p <prompt> -o json}. The
 * Gemini API key of the session goes to the child only as the environment variable
 * GEMINI_API_KEY. The free tier has no price: the cost of a step is recorded as 0 and the tokens
 * are recorded as the CLI reports them.
 */
class GeminiCliBuilder implements RealBuilder {

    /** The shell commands a step may run; everything else is denied by the policy of the CLI. */
    private static final List<String> SHELL_ALLOWED = List.of("./check.sh", "./gradlew", "npm", "cd",
            "git status", "git diff", "curl", "ls", "cat", "mkdir", "sleep", "kill");

    /** Exit code of the CLI when the session ended because the turn limit was reached. */
    private static final int EXIT_TURN_LIMIT = 53;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SessionKey sessionKey;
    private final ExecutionSettings settings;
    private final ToolProbe probe;

    GeminiCliBuilder(SessionKey sessionKey, ExecutionSettings settings, ToolProbe probe) {
        this.sessionKey = sessionKey;
        this.settings = settings;
        this.probe = probe;
    }

    @Override
    public Harness harness() {
        return Harness.GEMINI_CLI;
    }

    @Override
    public BuilderState state() {
        ToolProbe.Installed tool = probe.installed(settings.geminiCmd());
        boolean keySet = sessionKey.geminiKey().isPresent();
        List<BuilderState.Missing> missing = new ArrayList<>();
        if (!tool.installed()) {
            missing.add(new BuilderState.Missing("gemini_not_installed", "Gemini CLI is not installed:"
                    + " the command \"" + settings.geminiCmd() + "\" did not run on this machine."
                    + " Install it with ci/run install-gemini or set ENGINE_GEMINI_CMD.", null));
        }
        if (!keySet) {
            missing.add(new BuilderState.Missing("no_gemini_key", "Gemini CLI needs the Gemini API key."
                    + " Choose Google Gemini in the key panel and save the key (Google AI Studio,"
                    + " aistudio.google.com/apikey); the engine keeps it in memory only.", null));
        }
        return new BuilderState(harness().id(), harness().label(), missing.isEmpty(), tool.installed(),
                tool.version(), null, keySet, null, settings.geminiModel(), List.copyOf(missing));
    }

    @Override
    public StepOutcome run(StepRequest request) {
        Path out = request.logDir().resolve(request.stepKey() + ".json");
        Path err = request.logDir().resolve(request.stepKey() + ".err");
        long start = System.nanoTime();
        Path home = settings.workspaceRoot().resolve(".gemini-home");
        List<String> command = List.of(settings.geminiCmd(), "-p",
                BuilderSupport.prompt("GEMINI.md", request.prompt()), "-m", settings.geminiModel(),
                "-o", "json", "--approval-mode", "auto_edit", "--policy", writePolicy(home).toString(),
                "--skip-trust");
        ProcessRunner.Result result = ProcessRunner.run(command, request.workspace(),
                childEnv(request, home), out, err, settings.stepTimeout(), request.cancelled());
        int seconds = (int) ((System.nanoTime() - start) / 1_000_000_000L);
        return outcome(result, out, err, seconds);
    }

    /** The CLI keeps its own files in a folder of the engine, not in the home of the owner. */
    private Map<String, String> childEnv(StepRequest request, Path home) {
        Map<String, String> env = new HashMap<>(request.appEnv());
        env.put("GEMINI_CLI_HOME", home.toString());
        sessionKey.geminiKey().ifPresent(key -> env.put("GEMINI_API_KEY", key));
        return env;
    }

    /** Shell commands outside the list are denied; file tools work inside the workspace only. */
    private static Path writePolicy(Path home) {
        StringBuilder policy = new StringBuilder("[[rule]]\ntoolName = \"run_shell_command\"\n"
                + "commandPrefix = [");
        policy.append(String.join(", ", SHELL_ALLOWED.stream().map(c -> "\"" + c + "\"").toList()));
        policy.append("]\ndecision = \"allow\"\npriority = 100\n");
        try {
            Files.createDirectories(home);
            Path file = home.resolve("policy.toml");
            Files.writeString(file, policy.toString());
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the policy of the Gemini CLI to " + home, e);
        }
    }

    private StepOutcome outcome(ProcessRunner.Result result, Path out, Path err, int seconds) {
        String raw = read(out);
        String errors = read(err);
        if (result.cancelled()) {
            return failed(seconds, "cancelled", "The step was ended because the client stopped the run");
        }
        if (result.timedOut()) {
            String limit = LimitDetector.classify(errors + raw);
            return failed(seconds, limit != null ? limit : "timeout", limit != null
                    ? "Gemini CLI was still waiting for the quota when the step time ran out"
                    : "The step took longer than " + settings.stepTimeout().toMinutes()
                            + " minutes and was stopped");
        }
        try {
            return parsed(JSON.readTree(raw), result.exitCode(), errors, seconds);
        } catch (RuntimeException e) {
            String limit = LimitDetector.classify(errors + raw);
            return failed(seconds, limit != null ? limit : "no_json",
                    "Gemini CLI gave no readable answer (exit code " + result.exitCode() + "); see "
                            + err.getFileName());
        }
    }

    private StepOutcome parsed(JsonNode json, int exitCode, String errors, int seconds) {
        JsonNode error = json.path("error");
        boolean ok = exitCode == 0 && error.isMissingNode();
        int turns = 0;
        long tokens = 0;
        for (JsonNode model : json.path("stats").path("models")) {
            turns += model.path("api").path("totalRequests").asInt(0);
            tokens += model.path("tokens").path("total").asLong(0);
        }
        String message = error.path("message").asString("");
        String limit = ok ? null : LimitDetector.classify(message + errors);
        String subtype = ok ? "success" : exitCode == EXIT_TURN_LIMIT ? "error_max_turns" : "error";
        String summary = ok ? json.path("response").asString("")
                : message.isBlank() ? "Gemini CLI failed with exit code " + exitCode : message;
        return new StepOutcome(ok, BigDecimal.ZERO, turns, seconds, limit != null ? limit : subtype,
                oneLine(summary), tokens);
    }

    @Override
    public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir) {
        return BuilderSupport.gate(settings, workspace, appEnv, logDir);
    }
}
