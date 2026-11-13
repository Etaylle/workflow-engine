package engine.execution.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Metrics of a run (workflow.md 7.9).
 * Table: execution.run_metrics. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record RunMetrics(
        UUID runId,
        BigDecimal loopsPerTask,
        int firstTryTasks,
        int finishedTasks,
        int avgTaskMinutes,
        int waitingMinutes,
        Instant createdAt,
        Instant updatedAt) {
}
