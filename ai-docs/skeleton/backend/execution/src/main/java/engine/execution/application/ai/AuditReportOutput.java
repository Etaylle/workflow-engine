package engine.execution.application.ai;

import engine.execution.domain.Lens;
import engine.execution.domain.LensVerdict;
import engine.execution.domain.Severity;
import java.util.List;

/**
 * Structured output of the AUDITOR (step "Review and audit" per task, and once for the final report).
 * Maps to execution.audit_reports (three lenses, accepted risks, not verified) and execution.findings.
 * Validation: every finding has a severity, a lens and evidence text; "notVerified" is never empty (AUD-053).
 */
public record AuditReportOutput(LensVerdict technical, LensVerdict tests, LensVerdict business,
        List<FindingDraft> findings, List<String> acceptedRisks, String notVerified) {

    public record FindingDraft(Severity severity, Lens lens, String text, String requirementKey, String ownerRoleKey) {
    }
}
