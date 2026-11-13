package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable job table of the orchestrator (ADR-018).
 * Table: execution.jobs. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Job(
        UUID id,
        String kind,
        String payload,
        JobStatus status,
        int attempts,
        Instant availableAt,
        Instant leaseUntil,
        Instant createdAt,
        Instant updatedAt) {
}
