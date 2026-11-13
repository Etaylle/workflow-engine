package engine.documents.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An epic = one domain of the client software; E-0 is the template setup.
 * Table: documents.epics. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Epic(
        UUID id,
        UUID projectId,
        String key,
        String name,
        String domainName,
        int seq,
        Instant createdAt,
        Instant updatedAt) {
}
