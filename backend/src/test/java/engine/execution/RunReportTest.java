package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RunReportTest {

    private static final Instant START = Instant.parse("2026-11-15T10:00:00Z");

    private static RunView run(Instant finished) {
        RunView.Step done = new RunView.Step(UUID.randomUUID(), 1, "plan", "Plan", "done", "ok",
                new BigDecimal("0.40"), 5, 90, 1, "0123456789abcdef", 0, false, "claude_code", "fable", "high");
        RunView.Step failed = new RunView.Step(UUID.randomUUID(), 2, "build", "Build", "failed", "x",
                new BigDecimal("1.10"), 9, 300, 3, null, 0, false, null, null, null);
        return new RunView(UUID.randomUUID(), UUID.randomUUID(), 1, "claude_code", "fable", "high", null, null, null, "waiting", null, "question", null,
                0, new BigDecimal("1.50"), new BigDecimal("4.00"), 7, START, finished,
                List.of(new RunView.Task(UUID.randomUUID(), 1, "Order a meal", "System", "failed",
                        List.of(done, failed))));
    }

    @Test
    void countsComeFromTheRunData() {
        RunReport report = RunReport.of(run(START.plusSeconds(600)), List.of(), START.plusSeconds(9999));

        assertThat(report.seconds()).isEqualTo(600);
        assertThat(report.stepsDone()).isEqualTo(1);
        assertThat(report.stepsTotal()).isEqualTo(2);
        assertThat(report.tasksDone()).isZero();
        assertThat(report.attempts()).isEqualTo(4);
        assertThat(report.checkpoints()).isEqualTo(1);
    }

    @Test
    void aRunUnderWayCountsMinutesUpToNow() {
        RunReport report = RunReport.of(run(null), List.of(), START.plusSeconds(120));

        assertThat(report.seconds()).isEqualTo(120);
        assertThat(report.finishedAt()).isNull();
    }

    @Test
    void markdownHasTotalsStepsAndQuestions() {
        var asked = new RunReport.Question("Build", "Stuck. What now?", "retry", "Simplify", START);

        String md = RunReport.of(run(START.plusSeconds(600)), List.of(asked), START).toMarkdown();

        assertThat(md).contains("# Run report", "Minutes: 10.0", "Cost: USD 1.50 of USD 4.00",
                "| Plan | done | 1 | 0.40 | 90 | 01234567 |", "| Build | failed | 3 | 1.10 | 300 | - |",
                "Stuck. What now? Answer: retry (Simplify)");
    }

    @Test
    void markdownSaysWhenNothingWasAsked() {
        assertThat(RunReport.of(run(null), List.of(), START).toMarkdown()).contains("None.");
    }
}
