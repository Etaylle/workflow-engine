package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.ApiProblem;
import engine.shared.SessionKey;
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

/** The Gemini CLI builder against a fake gemini program: parsing, limits, prerequisites. */
class GeminiCliBuilderTest {

    @TempDir Path root;
    private Path bin;
    private Path workspace;
    private Path logs;
    private SessionKey keys;

    @BeforeEach
    void setUp() throws IOException {
        bin = Files.createDirectories(root.resolve("bin"));
        Path fake = bin.resolve("gemini");
        Files.copy(Path.of("src/test/resources/fake-cli/gemini"), fake, StandardCopyOption.REPLACE_EXISTING);
        assertThat(fake.toFile().setExecutable(true)).isTrue();
        workspace = Files.createDirectories(root.resolve("ws/run-1"));
        logs = Files.createDirectories(root.resolve("ws/run-1-logs"));
        keys = new SessionKey();
        keys.setCompatible("AIzaSyFAKE-unit-key-0123456789", "https://generativelanguage.googleapis.com/v1beta/openai/",
                "gemini-flash-lite-latest");
    }

    private GeminiCliBuilder builder(String command) {
        ExecutionSettings settings = new ExecutionSettings(root.resolve("ws"), "claude", "sonnet",
                command, "gemini-3.1-flash-lite", false, false, BigDecimal.TEN, Duration.ofSeconds(30),
                Duration.ofSeconds(30), 2, 3, Duration.ofMillis(10), Duration.ofMillis(10), 1, Duration.ofSeconds(30),
                Duration.ofSeconds(30));
        return new GeminiCliBuilder(keys, settings, new ToolProbe());
    }

    private StepOutcome run(GeminiCliBuilder builder, String... queue) throws IOException {
        Path state = Files.createDirectories(root.resolve("ws/fake-gemini"));
        Files.writeString(state.resolve("queue"), String.join("\n", queue) + "\n");
        return builder.run(new StepRequest(workspace, "build", "Do the work", BigDecimal.ONE, Map.of(),
                logs, () -> false, line -> { }));
    }

    @Test
    void aSuccessfulStepReportsTurnsTokensAndNoCost() throws IOException {
        StepOutcome outcome = run(builder(bin.resolve("gemini").toString()), "ok");

        assertThat(outcome.ok()).isTrue();
        assertThat(outcome.subtype()).isEqualTo("success");
        assertThat(outcome.turns()).isEqualTo(3);
        assertThat(outcome.tokens()).isEqualTo(1234);
        assertThat(outcome.costUsd()).isEqualByComparingTo("0");
        assertThat(outcome.summary()).isEqualTo("Did the step.");
    }

    @Test
    void aQuotaErrorIsALimitToWaitOutNotADefectToFix() throws IOException {
        StepOutcome outcome = run(builder(bin.resolve("gemini").toString()), "limit");

        assertThat(outcome.ok()).isFalse();
        assertThat(outcome.subtype()).isEqualTo("rate_limit");
        assertThat(outcome.limited()).isTrue();
    }

    @Test
    void anyOtherErrorIsAFailureForTheFixLoopWithTheMessageOfTheCli() throws IOException {
        StepOutcome outcome = run(builder(bin.resolve("gemini").toString()), "fail");

        assertThat(outcome.ok()).isFalse();
        assertThat(outcome.limited()).isFalse();
        assertThat(outcome.summary()).isEqualTo("The step went wrong: no luck");
    }

    @Test
    void theKeyReachesTheChildOnlyThroughTheEnvironment() throws IOException {
        run(builder(bin.resolve("gemini").toString()), "ok");

        assertThat(Files.readString(root.resolve("ws/fake-gemini/args.log"))).doesNotContain("AIzaSyFAKE");
        assertThat(Files.readString(root.resolve("ws/fake-gemini/env.log"))).contains("key=AIzaSyFAKE-unit-key");
    }

    @Test
    void withoutTheCommandOrTheKeyTheBuilderNamesWhatIsMissing() {
        GeminiCliBuilder builder = builder(root.resolve("no-such-gemini").toString());
        keys.clearAll();

        List<String> codes = builder.state().missing().stream().map(BuilderState.Missing::code).toList();

        assertThat(codes).containsExactly("gemini_not_installed", "no_gemini_key");
        assertThatThrownBy(builder::checkReady).isInstanceOf(ApiProblem.class)
                .hasMessageContaining("Gemini CLI is not installed").hasMessageContaining("Gemini API key");
    }

    @Test
    void anAnthropicOnlySessionDoesNotCountAsAGeminiKey() {
        keys.clearAll();
        keys.setAnthropic("sk-ant-fake-0123456789");

        assertThat(builder(bin.resolve("gemini").toString()).state().keySet()).isFalse();
    }

    @Test
    void aKeyOfAnotherCompatibleProviderDoesNotCountAsAGeminiKey() {
        keys.setCompatible("sk-fake-0123456789", "https://api.openai.com/v1", "gpt");

        assertThat(builder(bin.resolve("gemini").toString()).state().keySet()).isFalse();
    }
}
