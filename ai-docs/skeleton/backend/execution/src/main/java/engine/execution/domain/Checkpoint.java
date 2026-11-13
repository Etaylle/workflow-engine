package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A git commit after a green task, and its push to GitHub (step "Merge and push").
 * Table: execution.checkpoints. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Checkpoint(
        UUID id,
        UUID runId,
        UUID taskRunId,
        int seq,
        String commitSha,
        PushStatus pushStatus,
        Instant pushedAt,
        String commitUrl,
        Instant createdAt,
        Instant updatedAt) {
}
