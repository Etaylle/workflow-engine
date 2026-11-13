package engine.settings.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One step every task runs (editable, ordered by seq).
 * Table: settings.dev_workflow_steps. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record DevWorkflowStep(
        UUID id,
        UUID profileId,
        int seq,
        String key,
        String name,
        RunsWhen runsWhen,
        List<String> storyTypes,
        String doneWhen,
        Integer timeLimitMinutes,
        FixedPosition fixedPosition,
        Instant createdAt,
        Instant updatedAt) {
}
