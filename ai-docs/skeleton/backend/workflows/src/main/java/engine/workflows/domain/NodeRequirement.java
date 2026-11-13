package engine.workflows.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Which requirements a task implements (key only: no cross-schema join).
 * Table: workflows.node_requirements. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record NodeRequirement(
        UUID nodeId,
        String requirementKey,
        Instant createdAt) {
}
