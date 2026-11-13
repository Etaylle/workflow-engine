package engine.workflows.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Frozen copy of wish, answers, stack, AI, requirements, workflow, team and development workflow (insert-only).
 * Table: workflows.contracts. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Contract(
        UUID id,
        UUID projectId,
        int contractVersion,
        String snapshot,
        BigDecimal costLimitAmount,
        String costLimitCurrency,
        Instant confirmedAt,
        Instant createdAt) {
}
