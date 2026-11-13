package engine.documents.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Rendered requirements and project plan (insert-only).
 * Table: documents.generated_documents. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record GeneratedDocument(
        UUID id,
        UUID projectId,
        DocumentKind kind,
        int docVersion,
        DocumentFormat fileFormat,
        byte[] content,
        Instant createdAt) {
}
