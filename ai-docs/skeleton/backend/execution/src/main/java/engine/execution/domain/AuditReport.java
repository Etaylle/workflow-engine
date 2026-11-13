package engine.execution.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Audit report of a run: three lenses, risks, what was not verified.
 * Table: execution.audit_reports. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record AuditReport(
        UUID id,
        UUID runId,
        boolean isFinal,
        LensVerdict technical,
        LensVerdict tests,
        LensVerdict business,
        List<String> acceptedRisks,
        String notVerified,
        Instant createdAt,
        Instant updatedAt) {
}
