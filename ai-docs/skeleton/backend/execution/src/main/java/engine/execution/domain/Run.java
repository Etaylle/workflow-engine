package engine.execution.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One execution of a confirmed contract (aggregate root).
 * Table: execution.runs. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Run(
        UUID id,
        UUID projectId,
        UUID contractId,
        RunStatus status,
        int resumeCount,
        BigDecimal costUsedAmount,
        String sandboxRef,
        Instant startedAt,
        Instant endedAt,
        int version,
        Instant createdAt,
        Instant updatedAt) {
}
