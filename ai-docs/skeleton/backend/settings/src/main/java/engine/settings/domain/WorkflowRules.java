package engine.settings.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Rules for every task: fix loops before asking a human, CI stages, time.
 * Table: settings.workflow_rules. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record WorkflowRules(
        UUID profileId,
        int maxFixLoops,
        List<String> ciStages,
        int timeTargetMinutes,
        int ceilingMinutes,
        boolean raiseModelBeforeAsking,
        int temporaryRetries,
        boolean rebuildEnvironmentOnce,
        boolean setAsideFlakyTests,
        Instant createdAt,
        Instant updatedAt) {
}
