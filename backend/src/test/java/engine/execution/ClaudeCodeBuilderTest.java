package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import engine.shared.SessionKey;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** How Claude Code is started for a step, and what a step that ended without an answer says. */
class ClaudeCodeBuilderTest {

    private static final Path WS = Path.of("ws/run-1");

    private static ClaudeCodeBuilder builder() {
        ExecutionSettings settings = new ExecutionSettings(Path.of("ws"), "claude", "fable", "gemini",
                "gemini-3.1-flash-lite", "codex", "gpt-6.1-sol", false, false, Duration.ofMinutes(60),
                Duration.ofSeconds(30), 2, 3, Duration.ofMillis(10), Duration.ofMillis(10), 1,
                Duration.ofSeconds(30), Duration.ofSeconds(30), new BigDecimal("4"));
        return new ClaudeCodeBuilder(new SessionKey(), settings, new ClaudeAuth(settings), new ToolProbe());
    }

    private static StepRequest request(BigDecimal cap, String effort, String resumeSession) {
        return new StepRequest(WS, "plan", "Step PLAN. Write the plan.", cap, null, effort, Map.of(),
                Path.of("ws/run-1-logs"), () -> false, line -> { }, "task-01-plan-attempt-1", resumeSession);
    }

    @Test
    void aStepWithoutACapIsStartedWithoutABudgetAndKeepsItsSession() {
        List<String> command = builder().command(request(null, null, null));

        assertThat(command).doesNotContain("--max-budget-usd", "--no-session-persistence", "--resume",
                "--effort");
        assertThat(command).containsSequence("--model", "fable");
        assertThat(command.get(2)).startsWith("You are one step of a task build")
                .contains("Step PLAN. Write the plan.").contains("Shell rules of this workspace")
                .contains("head, tail, wc, grep");
    }

    @Test
    void aCapAndAnEffortArePassedOnWhenTheClientSetThem() {
        List<String> command = builder().command(request(new BigDecimal("2.50"), "medium", null));

        assertThat(command).containsSequence("--max-budget-usd", "2.50")
                .containsSequence("--effort", "medium");
    }

    @Test
    void aFurtherAttemptContinuesTheSessionWithOnlyWhatWentWrong() {
        List<String> command = builder().command(request(null, null, "0f1e-session"));

        assertThat(command).containsSequence("--resume", "0f1e-session");
        assertThat(command.get(2)).isEqualTo("Step PLAN. Write the plan.");
    }

    @Test
    void theBuilderMayReadAndMeasureFilesWithoutBeingRefused() {
        String allowed = builder().command(request(null, null, null)).getLast();

        assertThat(allowed).contains("Bash(head:*)", "Bash(wc:*)", "Bash(grep:*)", "Bash(command -v:*)",
                "Bash(./check.sh:*)").doesNotContain("Bash(rm:", "Bash(env:", "Bash(find:");
    }

    @Test
    void aStepStoppedAtItsBudgetSaysWhichBudgetAndWhatWasSpent() {
        String reason = ClaudeCodeBuilder.stopReason(StepOutcome.BUDGET,
                request(new BigDecimal("0.80"), null, null), new BigDecimal("0.8405"), Path.of("x.jsonl"));

        assertThat(reason).isEqualTo("the step stopped at its budget of USD 0.80 after spending USD 0.84,"
                + " before it was finished");
    }

    @Test
    void anyOtherEndWithoutAnAnswerNamesItsKindTheCostAndTheLog() {
        assertThat(ClaudeCodeBuilder.stopReason("error_max_turns", request(null, null, null),
                new BigDecimal("1.2"), Path.of("x.jsonl"))).contains("turn limit").contains("USD 1.20");
        assertThat(ClaudeCodeBuilder.stopReason("error_during_execution", request(null, null, null),
                BigDecimal.ZERO, Path.of("logs/task-01-plan-attempt-1.jsonl")))
                .contains("error_during_execution").contains("task-01-plan-attempt-1.jsonl");
    }

    @Test
    void theSessionOfAStreamIsReadFromItsFirstLineThatNamesOne() {
        assertThat(BuilderStream.sessionId("not json\n{\"type\":\"system\",\"session_id\":\"abc-1\"}\n"
                + "{\"type\":\"result\",\"session_id\":\"abc-1\"}")).isEqualTo("abc-1");
        assertThat(BuilderStream.sessionId("{\"type\":\"system\"}")).isNull();
        assertThat(BuilderStream.sessionId("")).isNull();
    }
}
