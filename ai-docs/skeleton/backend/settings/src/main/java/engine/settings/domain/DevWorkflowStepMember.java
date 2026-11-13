package engine.settings.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Which members do or check a step.
 * Table: settings.dev_workflow_step_members. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record DevWorkflowStepMember(
        UUID stepId,
        UUID memberId,
        MemberPart part,
        Instant createdAt) {
}
