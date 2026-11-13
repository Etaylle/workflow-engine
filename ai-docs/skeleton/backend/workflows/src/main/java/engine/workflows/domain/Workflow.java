package engine.workflows.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The business workflow of a project (aggregate root).
 * Table: workflows.workflows. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Workflow(
        UUID id,
        UUID projectId,
        WorkflowStatus status,
        int version,
        Instant createdAt,
        Instant updatedAt) {
}
