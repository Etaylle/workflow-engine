package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.ApiProblem;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The Codex CLI builder against a fake codex program: parsing, limits, arguments, prerequisites. */
class CodexCliBuilderTest {

    @TempDir Path root;
    private Path bin;
    private Path workspace;
    private Path logs;

    @BeforeEach
    void setUp() throws IOException {
        bin = Files.createDirectories(root.resolve("bin"));
        Path fake = bin.resolve("codex");
        Files.copy(Path.of("src/test/resources/fake-cli/codex"), fake, StandardCopyOption.REPLACE_EXISTING);
        assertThat(fake.toFile().setExecutable(true)).isTrue();
        workspace = Files.createDirectories(root.resolve("ws/run-1"));
        logs = Files.createDirectories(root.resolve("ws/run-1-logs"));
    }

    private CodexCliBuilder builder(String command) {
        ExecutionSettings settings = new ExecutionSettings(root.resolve("ws"), "claude", "sonnet",
                "gemini", "gemini-3.1-flash-lite", command, "gpt-6.1-sol", false, false,
                Duration.ofSeconds(30), Duration.ofSeconds(30), 2, 3, Duration.ofMillis(10),
                Duration.ofMillis(10), 1, Duration.ofSeconds(30), Duration.ofSeconds(30), new BigDecimal("4"));
        return new CodexCliBuilder(settings, new ToolProbe());
    }

    private StepOutcome run(String model, String effort, String... queue) throws IOException {
        Path state = Files.createDirectories(bin.resolve("fake-codex"));
        Files.writeString(state.resolve("queue"), String.join("\n", queue) + "\n");
        return builder(bin.resolve("codex").toString()).run(new StepRequest(workspace, "build", "Do the work",
                BigDecimal.ONE, model, effort, Map.of(), logs, () -> false, line -> { }, "build", null));
    }

    @Test
    void aSuccessfulStepReportsToolCallsTokensAndNoCost() throws IOException {
        StepOutcome outcome = run(null, null, "ok");

        assertThat(outcome.ok()).isTrue();
        assertThat(outcome.subtype()).isEqualTo("success");
        assertThat(outcome.turns()).isEqualTo(3);
        assertThat(outcome.tokens()).isEqualTo(1234);
        assertThat(outcome.costUsd()).isEqualByComparingTo("0");
        assertThat(outcome.summary()).isEqualTo("Did the step.");
    }

    @Test
    void theUnattendedCommandLineHasSandboxNoApprovalsAndTheGuideFileOfCodex() throws IOException {
        run("gpt-6-luna", "high", "ok");

        String args = Files.readString(bin.resolve("fake-codex/args.log")).stripTrailing();
        assertThat(args).startsWith("exec --json --ephemeral --skip-git-repo-check --ignore-user-config"
                + " --ignore-rules -m gpt-6-luna -c model_reasoning_effort=\"high\" -s workspace-write"
                + " -c approval_policy=\"never\" -c sandbox_workspace_write.network_access=true -C "
                + workspace).contains("Read AGENTS.md and CONTRACT.md").endsWith("Do the work");
    }

    @Test
    void withoutAChoiceTheDefaultModelIsUsedAndNoEffortIsPassed() throws IOException {
        run(null, null, "ok");

        assertThat(Files.readString(bin.resolve("fake-codex/args.log"))).contains("-m gpt-6.1-sol")
                .doesNotContain("model_reasoning_effort");
    }

    @Test
    void noKeyOfTheEngineSessionReachesTheChild() throws IOException {
        run(null, null, "ok");

        assertThat(Files.readString(bin.resolve("fake-codex/env.log"))).contains("openai= gemini= anthropic=");
    }

    @Test
    void aUsageLimitIsALimitToWaitOutNotADefectToFix() throws IOException {
        StepOutcome outcome = run(null, null, "limit");

        assertThat(outcome.ok()).isFalse();
        assertThat(outcome.subtype()).isEqualTo("rate_limit");
        assertThat(outcome.limited()).isTrue();
    }

    @Test
    void anyOtherErrorIsAFailureForTheFixLoopWithTheMessageOfTheCli() throws IOException {
        StepOutcome outcome = run(null, null, "fail");

        assertThat(outcome.ok()).isFalse();
        assertThat(outcome.limited()).isFalse();
        assertThat(outcome.summary()).isEqualTo("The step went wrong: no luck");
    }

    @Test
    void notSignedInIsMissingWithTheLoginCommandAndSignedInIsReady() throws IOException {
        String command = bin.resolve("codex").toString();

        BuilderState out = builder(command).state();
        assertThat(out.signedIn()).isEqualTo("no");
        assertThat(out.ready()).isFalse();
        assertThat(out.missing()).extracting(BuilderState.Missing::code).containsExactly("codex_not_signed_in");
        assertThat(out.missing().get(0).message()).contains(command + " login");
        assertThatThrownBy(builder(command)::checkReady).isInstanceOf(ApiProblem.class)
                .hasMessageContaining("Codex CLI cannot build yet");

        Files.writeString(bin.resolve("codex-logged-in"), "yes");
        BuilderState in = builder(command).state();
        assertThat(in.signedIn()).isEqualTo("yes");
        assertThat(in.ready()).isTrue();
        assertThat(in.efforts()).containsExactly("low", "medium", "high", "xhigh");
    }

    @Test
    void aMissingCommandIsNotInstalled() {
        BuilderState state = builder(root.resolve("no-such-codex").toString()).state();

        assertThat(state.installed()).isFalse();
        assertThat(state.missing()).extracting(BuilderState.Missing::code).containsExactly("codex_not_installed");
        assertThat(List.of(state.signedIn())).containsExactly("unknown");
    }
}
