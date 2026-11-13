package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evidence: verdict of each member in step Team verification.
 * Table: execution.verdicts. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record MemberVerdict(
        UUID id,
        UUID taskRunId,
        String roleKey,
        Verdict verdict,
        String reason,
        Instant createdAt) {
}
