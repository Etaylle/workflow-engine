package engine.execution.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One continuous integration run (ci/run all-local).
 * Table: execution.ci_runs. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record CiRun(
        UUID id,
        UUID taskRunId,
        int number,
        CiRunStatus status,
        Map<String, String> stages,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt,
        Instant updatedAt) {
}
