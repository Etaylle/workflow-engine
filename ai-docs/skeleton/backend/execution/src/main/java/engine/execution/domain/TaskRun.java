package engine.execution.domain;

import engine.shared.StoryType;
import java.time.Instant;
import java.util.UUID;

/**
 * One business task in a run (box on the canvas).
 * Table: execution.task_runs. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record TaskRun(
        UUID id,
        UUID runId,
        UUID nodeId,
        String storyKey,
        int seq,
        StoryType storyType,
        TaskRunStatus status,
        int fixLoops,
        boolean modelRaised,
        String summary,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt,
        Instant updatedAt) {
}
