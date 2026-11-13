package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An error, its type, the automatic reaction and how it ended.
 * Table: execution.run_errors. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record RunError(
        UUID id,
        UUID taskRunId,
        UUID stepRunId,
        UUID ciRunId,
        ErrorClass errorClass,
        String message,
        String detail,
        RunErrorStatus status,
        int attempts,
        String autoAction,
        Integer fixedInLoop,
        Instant createdAt,
        Instant updatedAt) {
}
