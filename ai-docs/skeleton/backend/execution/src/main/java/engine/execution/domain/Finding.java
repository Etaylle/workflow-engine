package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One audit finding.
 * Table: execution.findings. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Finding(
        UUID id,
        UUID auditReportId,
        Severity severity,
        Lens lens,
        String text,
        String requirementKey,
        String ownerRoleKey,
        FindingStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
