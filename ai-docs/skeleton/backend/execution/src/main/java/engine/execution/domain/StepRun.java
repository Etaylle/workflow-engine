package engine.execution.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One development workflow step of one task.
 * Table: execution.step_runs. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record StepRun(
        UUID id,
        UUID taskRunId,
        int seq,
        String stepKey,
        String stepName,
        List<String> memberRoleKeys,
        StepRunStatus status,
        String result,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt,
        Instant updatedAt) {
}
