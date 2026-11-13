package engine.workflows.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A sequence flow arrow.
 * Table: workflows.edges. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Edge(
        UUID id,
        UUID workflowId,
        UUID sourceNodeId,
        UUID targetNodeId,
        Instant createdAt) {
}
