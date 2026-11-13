package engine.documents.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One requirement with ID, priority and source.
 * Table: documents.requirements. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Requirement(
        UUID id,
        UUID projectId,
        UUID epicId,
        String key,
        RequirementType type,
        String text,
        Priority priority,
        String source,
        Instant createdAt,
        Instant updatedAt) {
}
