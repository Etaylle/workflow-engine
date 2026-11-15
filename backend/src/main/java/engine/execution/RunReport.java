package engine.execution;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The final report of a run, built only from the run's own data: tasks, steps, attempts, cost,
 * minutes, checkpoints and the questions asked of the client.
 */
public record RunReport(
        java.util.UUID runId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        long seconds,
        BigDecimal costUsd,
        BigDecimal costLimitUsd,
        int resumeCount,
        int tasksDone,
        int tasksTotal,
        int stepsDone,
        int stepsTotal,
        int attempts,
        int checkpoints,
        List<Task> tasks,
        List<Question> questions) {

    /** One task with its steps. */
    public record Task(String title, String status, List<Step> steps) {
    }

    /** One step: attempts are the Claude Code invocations, commit the checkpoint if any. */
    public record Step(String title, String status, int attempts, BigDecimal costUsd, int seconds,
            String commit) {
    }

    /** One question the run asked the client; choice and hint are null until answered. */
    public record Question(String stepTitle, String question, String choice, String hint,
            Instant askedAt) {
    }

    /** Builds the report of a run; a run under way counts its time up to {@code now}. */
    static RunReport of(RunView run, List<Question> questions, Instant now) {
        List<Task> tasks = run.tasks().stream()
                .map(t -> new Task(t.title(), t.status(), t.steps().stream()
                        .map(s -> new Step(s.title(), s.status(), s.attempts(), s.costUsd(),
                                s.seconds(), s.commit()))
                        .toList()))
                .toList();
        List<Step> steps = tasks.stream().flatMap(t -> t.steps().stream()).toList();
        Instant end = run.finishedAt() == null ? now : run.finishedAt();
        return new RunReport(run.id(), run.status(), run.startedAt(), run.finishedAt(),
                Duration.between(run.startedAt(), end).toSeconds(), run.costUsd(),
                run.costLimitUsd(), run.resumeCount(),
                (int) tasks.stream().filter(t -> "done".equals(t.status())).count(), tasks.size(),
                (int) steps.stream().filter(s -> "done".equals(s.status())).count(), steps.size(),
                steps.stream().mapToInt(Step::attempts).sum(),
                (int) steps.stream().filter(s -> s.commit() != null).count(), tasks, questions);
    }

    /** The report as Markdown, for the ZIP. */
    String toMarkdown() {
        StringBuilder md = new StringBuilder("# Run report\n\n");
        md.append("- Status: ").append(status).append('\n')
                .append("- Started: ").append(startedAt).append('\n')
                .append("- Finished: ").append(finishedAt == null ? "not yet" : finishedAt.toString())
                .append('\n')
                .append("- Minutes: ").append(String.format(java.util.Locale.ROOT, "%.1f", seconds / 60.0))
                .append('\n')
                .append("- Cost: USD ").append(costUsd).append(" of USD ").append(costLimitUsd)
                .append('\n')
                .append("- Tasks done: ").append(tasksDone).append(" of ").append(tasksTotal).append('\n')
                .append("- Steps done: ").append(stepsDone).append(" of ").append(stepsTotal).append('\n')
                .append("- Claude Code invocations: ").append(attempts).append('\n')
                .append("- Checkpoints (git commits): ").append(checkpoints).append('\n')
                .append("- Resumed after an engine restart: ").append(resumeCount).append(" times\n");
        for (Task task : tasks) {
            md.append("\n## ").append(task.title()).append(" (").append(task.status()).append(")\n\n")
                    .append("| Step | Status | Attempts | Cost (USD) | Seconds | Checkpoint |\n")
                    .append("|---|---|---|---|---|---|\n");
            for (Step step : task.steps()) {
                md.append("| ").append(step.title()).append(" | ").append(step.status())
                        .append(" | ").append(step.attempts()).append(" | ").append(step.costUsd())
                        .append(" | ").append(step.seconds()).append(" | ")
                        .append(step.commit() == null ? "-" : step.commit().substring(0, Math.min(8, step.commit().length())))
                        .append(" |\n");
            }
        }
        md.append("\n## Questions asked of the client\n\n");
        if (questions.isEmpty()) {
            md.append("None.\n");
        }
        for (Question q : questions) {
            md.append("- ").append(q.stepTitle()).append(": ").append(q.question()).append(" Answer: ")
                    .append(q.choice() == null ? "open" : q.choice())
                    .append(q.hint() == null || q.hint().isBlank() ? "" : " (" + q.hint() + ")")
                    .append('\n');
        }
        return md.toString();
    }
}
