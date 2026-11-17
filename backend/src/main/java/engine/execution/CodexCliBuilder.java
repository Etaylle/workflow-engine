package engine.execution;

import static engine.execution.BuilderSupport.failed;
import static engine.execution.BuilderSupport.oneLine;
import static engine.execution.BuilderSupport.read;

import engine.shared.BuilderChoice;
import engine.shared.Harness;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Codex CLI headless (D-102), one invocation per step: {@code codex exec --json}. The builder uses
 * the sign-in of the machine ({@code codex login}, kept by the CLI in its own folder); the engine
 * passes no key, and the Gemini key of the planning slot is never given to Codex. A ChatGPT plan
 * has no price per call: the cost of a step is recorded as 0 and the tokens as the CLI reports them.
 * The step runs in the workspace-write sandbox of the CLI (writes only inside the workspace, network
 * on for package installs) and is never asked for an approval.
 */
class CodexCliBuilder implements RealBuilder {

    /** Suggested model ids (the Codex models page, 2026-11); any other id may be typed. */
    static final List<BuilderState.ModelOption> MODELS = List.of(
            new BuilderState.ModelOption("gpt-6-luna", "Most efficient, for focused tasks"),
            new BuilderState.ModelOption("gpt-6.1-sol", "Near the top model, lower cost"),
            new BuilderState.ModelOption("gpt-6-astra", "Most capable"));

    private final ExecutionSettings settings;
    private final ToolProbe probe;

    CodexCliBuilder(ExecutionSettings settings, ToolProbe probe) {
        this.settings = settings;
        this.probe = probe;
    }

    @Override
    public Harness harness() {
        return Harness.CODEX_CLI;
    }

    @Override
    public BuilderState state() {
        ToolProbe.Installed tool = probe.installed(settings.codexCmd());
        String signedIn = tool.installed() ? probe.codexSignedIn(settings.codexCmd())
                .map(in -> in ? "yes" : "no").orElse("unknown") : "unknown";
        List<BuilderState.Missing> missing = new ArrayList<>();
        if (!tool.installed()) {
            missing.add(new BuilderState.Missing("codex_not_installed", "Codex CLI is not installed:"
                    + " the command \"" + settings.codexCmd() + "\" did not run on this machine."
                    + " Install it with ci/run install-codex or set ENGINE_CODEX_CMD.", null));
        } else if ("no".equals(signedIn)) {
            missing.add(new BuilderState.Missing("codex_not_signed_in", "Codex CLI is not signed in on"
                    + " this machine. Run `" + settings.codexCmd() + " login` once in a terminal on this"
                    + " machine and sign in with ChatGPT.", "builders.codex.signInInstruction"));
        }
        return new BuilderState(harness().id(), harness().label(), missing.isEmpty(), tool.installed(),
                tool.version(), signedIn, false, null, settings.codexModel(), MODELS, true,
                BuilderChoice.CODEX_EFFORTS, null, List.copyOf(missing));
    }

    @Override
    public StepOutcome run(StepRequest request) {
        Path out = request.logDir().resolve(request.logName() + ".jsonl");
        Path err = request.logDir().resolve(request.logName() + ".err");
        long start = System.nanoTime();
        ProcessRunner.Result result = ProcessRunner.run(command(request), request.workspace(),
                request.appEnv(), out, err, settings.stepTimeout(), request.cancelled(),
                line -> BuilderStream.codex(line, request.workspace().toString())
                        .ifPresent(text -> request.console().accept(BuilderStream.redact(text, List.of()))));
        int seconds = (int) ((System.nanoTime() - start) / 1_000_000_000L);
        return outcome(result, read(out), read(err), err, seconds);
    }

    /** The unattended command line: no approvals, writes inside the workspace only, network on. */
    private List<String> command(StepRequest request) {
        List<String> command = new ArrayList<>(List.of(settings.codexCmd(), "exec", "--json", "--ephemeral",
                "--skip-git-repo-check", "--ignore-user-config", "--ignore-rules",
                "-m", request.model() != null ? request.model() : settings.codexModel()));
        if (request.effort() != null) {
            command.addAll(List.of("-c", "model_reasoning_effort=\"" + request.effort() + "\""));
        }
        command.addAll(List.of("-s", "workspace-write", "-c", "approval_policy=\"never\"",
                "-c", "sandbox_workspace_write.network_access=true",
                "-C", request.workspace().toString(), BuilderSupport.prompt("AGENTS.md", request.prompt())));
        return command;
    }

    private StepOutcome outcome(ProcessRunner.Result result, String raw, String errors, Path err, int seconds) {
        if (result.cancelled()) {
            return failed(seconds, "cancelled", "The step was ended because the client stopped the run");
        }
        if (result.timedOut()) {
            String limit = LimitDetector.classify(errors + raw);
            return failed(seconds, limit != null ? limit : "timeout", limit != null
                    ? "Codex CLI was still waiting for the usage limit when the step time ran out"
                    : "The step took longer than " + settings.stepTimeout().toMinutes()
                            + " minutes and was stopped");
        }
        BuilderStream.CodexSummary summary = BuilderStream.codexSummary(raw);
        if (result.exitCode() == 0 && summary.completed() && summary.failure().isBlank()) {
            return new StepOutcome(true, BigDecimal.ZERO, summary.toolCalls(), seconds, "success",
                    oneLine(summary.message()), summary.tokens());
        }
        String limit = LimitDetector.classify(summary.failure() + errors);
        if (!summary.completed() && summary.failure().isBlank() && limit == null) {
            return failed(seconds, "no_json", "Codex CLI gave no readable answer (exit code "
                    + result.exitCode() + "); see " + err.getFileName());
        }
        String text = summary.failure().isBlank() ? "Codex CLI failed with exit code " + result.exitCode()
                : summary.failure();
        return new StepOutcome(false, BigDecimal.ZERO, summary.toolCalls(), seconds,
                limit != null ? limit : "error", oneLine(text), summary.tokens());
    }

    @Override
    public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir,
            java.util.function.Consumer<String> console) {
        return BuilderSupport.gate(settings, workspace, appEnv, logDir, console);
    }
}
