package engine.settings.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A repeated cause across runs and the automatic check proposed for it (workflow.md 7.5).
 * Table: settings.lessons. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Lesson(
        UUID id,
        UUID profileId,
        String cause,
        int occurrences,
        int runCount,
        String proposal,
        String targetStepKey,
        LessonStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
