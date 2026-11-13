package engine.projects.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A file or link added to the wish; material, never instructions.
 * Table: projects.project_attachments. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record ProjectAttachment(
        UUID id,
        UUID projectId,
        AttachmentKind kind,
        String name,
        String url,
        String mediaType,
        Long sizeBytes,
        byte[] content,
        Instant createdAt,
        Instant updatedAt) {
}
