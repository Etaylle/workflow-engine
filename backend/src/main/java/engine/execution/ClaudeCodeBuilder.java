package engine.execution;

import static engine.execution.BuilderSupport.failed;
import static engine.execution.BuilderSupport.oneLine;
import static engine.execution.BuilderSupport.read;

import engine.shared.Harness;
import engine.shared.SessionKey;
import java.math.BigDecimal;
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
class ClaudeCodeBuilder implements RealBuilder {

    private static final String ALLOWED = "Read,Edit,Write,Glob,Grep,Bash(./check.sh:*),"
            + "Bash(./gradlew:*),Bash(npm:*),Bash(cd:*),Bash(git status:*),Bash(git diff:*),"
            + "Bash(curl:*),Bash(ls:*),Bash(cat:*),Bash(mkdir:*),Bash(sleep:*),Bash(kill:*)";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SessionKey sessionKey;
    private final ExecutionSettings settings;
    private final ClaudeAuth auth;
    private final ToolProbe probe;

    ClaudeCodeBuilder(SessionKey sessionKey, ExecutionSettings settings, ClaudeAuth auth, ToolProbe probe) {
        this.sessionKey = sessionKey;
        this.settings = settings;
        this.auth = auth;
        this.probe = probe;
    }

    @Override
    public Harness harness() {
        return Harness.CLAUDE_CODE;
    }

    @Override
    public BuilderState state() {
        ToolProbe.Installed tool = probe.installed(settings.claudeBin());
        boolean keySet = sessionKey.anthropicKey().isPresent();
        String signedIn = null;
        List<BuilderState.Missing> missing = new ArrayList<>();
        if (!tool.installed()) {
            missing.add(new BuilderState.Missing("claude_not_installed", "Claude Code is not installed:"
                    + " the command \"" + settings.claudeBin() + "\" did not run on this machine.", null));
        }
        if (auth.signIn()) {
            signedIn = tool.installed() ? probe.claudeSignedIn(settings.claudeBin())
                    .map(in -> in ? "yes" : "no").orElse("unknown") : "unknown";
            if ("no".equals(signedIn)) {
                missing.add(new BuilderState.Missing("not_signed_in", "Claude Code is not signed in on"
                        + " this machine. Run `claude` once in a terminal on this machine and log in.",
                        "builders.claude.signInInstruction"));
            }
        } else if (!keySet) {
            String other = sessionKey.compatible().isPresent()
                    ? " The key of the other provider is only used for planning, not for building."
                    : "";
            missing.add(new BuilderState.Missing("no_anthropic_key", "Claude Code needs an Anthropic"
                    + " API key (or the machine sign-in mode). Enter the key in the key panel; the"
                    + " engine keeps it in memory only." + other, null));
        }
        return new BuilderState(harness().id(), harness().label(), missing.isEmpty(), tool.installed(),
                tool.version(), signedIn, keySet, auth.mode().id(), settings.model(), List.copyOf(missing));
    }

    @Override
    public StepOutcome run(StepRequest request) {
        Path out = request.logDir().resolve(request.stepKey() + ".jsonl");
        Path err = request.logDir().resolve(request.stepKey() + ".err");
        long start = System.nanoTime();
        List<String> command = List.of(settings.claudeBin(), "-p",
                BuilderSupport.prompt("CLAUDE.md", request.prompt()),
                "--model", settings.model(), "--output-format", "stream-json", "--verbose", "--permission-mode",
                "acceptEdits", "--max-budget-usd", request.capUsd().toPlainString(),
                "--no-session-persistence", "--setting-sources", "project,local",
                "--strict-mcp-config", "--disable-slash-commands", "--tools",
                "Read,Edit,Write,Glob,Grep,Bash", "--allowedTools", ALLOWED);
        ProcessRunner.Result result = ProcessRunner.run(command, request.workspace(), childEnv(request),
                out, err, settings.stepTimeout(), request.cancelled(),
                line -> BuilderStream.claude(line, request.workspace().toString())
                        .ifPresent(text -> request.console().accept(redact(text))));
        int seconds = (int) ((System.nanoTime() - start) / 1_000_000_000L);
        return outcome(result, out, err, seconds);
    }

    private String redact(String text) {
        return BuilderStream.redact(text, java.util.stream.Stream.of(sessionKey.anthropicKey(),
                sessionKey.compatible().map(c -> c.apiKey())).flatMap(java.util.Optional::stream).toList());
    }

    private Map<String, String> childEnv(StepRequest request) {
        Map<String, String> env = new HashMap<>(request.appEnv());
        if (!auth.signIn()) {
            sessionKey.anthropicKey().ifPresent(key -> env.put("ANTHROPIC_API_KEY", key));
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
            JsonNode json = BuilderStream.lastResult(read(out));
            if (json == null) {
                throw new IllegalStateException("no result line in the stream");
            }
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

    @Override
    public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir,
            java.util.function.Consumer<String> console) {
        return BuilderSupport.gate(settings, workspace, appEnv, logDir, console);
    }
}
