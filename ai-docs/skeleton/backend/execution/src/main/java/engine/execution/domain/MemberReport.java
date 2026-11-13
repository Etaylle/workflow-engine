package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Task spec, report or blocker report of a member (insert-only).
 * Table: execution.member_reports. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record MemberReport(
        UUID id,
        UUID stepRunId,
        String roleKey,
        ReportKind kind,
        String content,
        Instant createdAt) {
}
