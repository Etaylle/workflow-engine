package engine.execution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A run as the API shows it: state, tasks with their steps, cost and the last event number. */
public record RunView(
        UUID id,
        UUID projectId,
        int contractVersion,
        String status,
        String failureReason,
        BigDecimal costUsd,
        BigDecimal costLimitUsd,
        long lastSeq,
        Instant startedAt,
        Instant finishedAt,
        List<Task> tasks) {

    /** One software task of the contract. */
    public record Task(UUID id, int seq, String title, String lane, String status, List<Step> steps) {
    }

    /** One development step of a task. */
    public record Step(UUID id, int seq, String key, String title, String status, String result,
            BigDecimal costUsd, int turns, int seconds, String commit) {
    }
}
