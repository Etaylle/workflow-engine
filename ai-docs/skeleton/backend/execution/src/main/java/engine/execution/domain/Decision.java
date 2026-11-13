package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A decision the AI took on its own (also DECISIONS.md).
 * Table: execution.decisions. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Decision(
        UUID id,
        UUID runId,
        UUID taskRunId,
        String madeBy,
        String title,
        String reason,
        Instant createdAt) {
}
