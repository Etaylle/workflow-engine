package engine.execution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A run as the API shows it: builder with model and effort, state, tasks with their steps, cost and the last event number. */
public record RunView(
        UUID id,
        UUID projectId,
        int contractVersion,
        String harness,
        String builderModel,
        String builderEffort,
        String reviewerHarness,
        String reviewerModel,
        String reviewerEffort,
        String status,
        String failureReason,
        String waitKind,
        Question question,
        int resumeCount,
        BigDecimal costUsd,
        BigDecimal costLimitUsd,
        long lastSeq,
        Instant startedAt,
        Instant finishedAt,
        List<Task> tasks) {

    /** The question a waiting run asks the client, with the short answers it accepts. */
    public record Question(UUID id, String text, String stepTitle, List<String> options) {
    }

    /** One software task of the contract. */
    public record Task(UUID id, int seq, String title, String lane, String status, List<Step> steps) {
    }

    /** One development step of a task; the builder fields say who ran its last attempt. */
    public record Step(UUID id, int seq, String key, String title, String status, String result,
            BigDecimal costUsd, int turns, int seconds, int attempts, String commit,
            long tokens, boolean review, String builderHarness, String builderModel,
            String builderEffort) {
    }
}
