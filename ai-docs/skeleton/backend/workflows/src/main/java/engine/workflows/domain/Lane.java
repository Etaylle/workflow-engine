package engine.workflows.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A BPMN lane = an actor (Student, Kitchen).
 * Table: workflows.lanes. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Lane(
        UUID id,
        UUID workflowId,
        String name,
        int seq,
        Instant createdAt,
        Instant updatedAt) {
}
