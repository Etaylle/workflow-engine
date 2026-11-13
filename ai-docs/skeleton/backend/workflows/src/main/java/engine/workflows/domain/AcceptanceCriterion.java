package engine.workflows.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * "Done when" of a task.
 * Table: workflows.acceptance_criteria. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record AcceptanceCriterion(
        UUID id,
        UUID nodeId,
        int seq,
        String text,
        Instant createdAt,
        Instant updatedAt) {
}
