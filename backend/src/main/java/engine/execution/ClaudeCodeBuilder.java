package engine.execution;

import static engine.execution.BuilderSupport.failed;
import static engine.execution.BuilderSupport.oneLine;
import static engine.execution.BuilderSupport.read;

import engine.shared.BuilderChoice;
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

    /** The shell commands the builder may run without asking; reading and plain file work only. */
    static final List<String> SHELL = List.of("./check.sh", "./start.sh", "./gradlew", "npm", "npx", "node",
            "python3", "cd", "git status", "git diff", "git log", "curl", "ls", "cat", "head", "tail", "wc",
            "grep", "sort", "uniq", "cut", "diff", "date", "which", "command -v", "echo", "printf",
            "pwd", "mkdir", "cp", "mv", "touch", "sleep", "kill");

    private static final String ALLOWED = "Read,Edit,Write,Glob,Grep,"
            + String.join(",", SHELL.stream().map(command -> "Bash(" + command + ":*)").toList());

    /** Told to Claude Code with every new step, so it does not lose turns to refused commands. */
    static final String SHELL_NOTE = "\n\nShell rules of this workspace: a shell call may use only these"
            + " commands, also after a pipe, && or a semicolon: " + String.join(", ", SHELL) + ". Loops,"
            + " shell variables, subshells in parentheses and a VAR=value prefix are refused, so keep"
            + " every call plain. Read, search and edit files with your file tools, and keep every file"
            + " inside the workspace (no /tmp).";

    /** The models to offer, cheapest first (the aliases of the CLI). */
    static final List<BuilderState.ModelOption> MODELS = List.of(
            new BuilderState.ModelOption("haiku", "Fastest and cheapest"),
            new BuilderState.ModelOption("sonnet", "Fast and balanced"),
            new BuilderState.ModelOption("opus", "Strong, slower and dearer"),
            new BuilderState.ModelOption("fable", "Most capable and most expensive"));

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
                tool.version(), signedIn, keySet, auth.mode().id(), settings.model(), MODELS, false,
                BuilderChoice.EFFORTS, null, List.copyOf(missing));
    }

    @Override
    public StepOutcome run(StepRequest request) {
        Path out = request.logDir().resolve(request.logName() + ".jsonl");
        Path err = request.logDir().resolve(request.logName() + ".err");
        long start = System.nanoTime();
        List<String> command = command(request);
        ProcessRunner.Result result = ProcessRunner.run(command, request.workspace(), childEnv(request),
                out, err, settings.stepTimeout(), request.cancelled(),
                line -> BuilderStream.claude(line, request.workspace().toString())
                        .ifPresent(text -> request.console().accept(redact(text))));
        int seconds = (int) ((System.nanoTime() - start) / 1_000_000_000L);
        return outcome(request, result, out, err, seconds).inSession(BuilderStream.sessionId(read(out)));
    }

    /**
     * The command line of one invocation. A new step gets the preamble, the step and the shell rules;
     * a further attempt continues the session of the previous one with only what went wrong. The
     * session is kept on disk so that it can be continued.
     */
    List<String> command(StepRequest request) {
        boolean resume = request.resumeSession() != null;
        List<String> command = new ArrayList<>(List.of(settings.claudeBin(), "-p",
                resume ? request.prompt() : BuilderSupport.prompt("CLAUDE.md", request.prompt()) + SHELL_NOTE,
                "--model", request.model() != null ? request.model() : settings.model()));
        if (resume) {
            command.addAll(List.of("--resume", request.resumeSession()));
        }
        if (request.effort() != null) {
            command.addAll(List.of("--effort", request.effort()));
        }
        if (request.capUsd() != null) {
            command.addAll(List.of("--max-budget-usd", request.capUsd().toPlainString()));
        }
        command.addAll(List.of("--output-format", "stream-json", "--verbose", "--permission-mode",
                "acceptEdits", "--setting-sources", "project,local", "--strict-mcp-config",
                "--disable-slash-commands", "--tools", "Read,Edit,Write,Glob,Grep,Bash", "--allowedTools",
                ALLOWED));
        return command;
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

    private StepOutcome outcome(StepRequest request, ProcessRunner.Result result, Path out, Path err,
            int seconds) {
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
            BigDecimal cost = BigDecimal.valueOf(json.path("total_cost_usd").asDouble(0));
            String said = oneLine(json.path("result").asString(""));
            return new StepOutcome(ok, cost, json.path("num_turns").asInt(0), seconds,
                    limit != null ? limit : subtype,
                    ok || !said.isBlank() ? said : stopReason(subtype, request, cost, out));
        } catch (RuntimeException e) {
            String limit = LimitDetector.classify(read(err) + read(out));
            return failed(seconds, limit != null ? limit : "no_json",
                    "Claude Code gave no readable answer (exit code " + result.exitCode() + "); see "
                            + err.getFileName());
        }
    }

    /** Why Claude Code ended a step without an answer, in words the client and the builder can act on. */
    static String stopReason(String subtype, StepRequest request, BigDecimal cost, Path log) {
        String spent = "USD " + cost.setScale(2, java.math.RoundingMode.HALF_UP);
        return switch (subtype) {
            case StepOutcome.BUDGET -> "the step stopped at its budget of USD " + request.capUsd()
                    + " after spending " + spent + ", before it was finished";
            case "error_max_turns" -> "the step stopped at the turn limit of the builder after spending "
                    + spent + ", before it was finished";
            default -> "the builder ended without an answer (" + subtype + ", " + spent
                    + " spent); its output is in " + log.getFileName();
        };
    }

    @Override
    public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir,
            java.util.function.Consumer<String> console) {
        return BuilderSupport.gate(settings, workspace, appEnv, logDir, console);
    }
}
