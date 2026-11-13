package engine.workflows.domain;

import engine.shared.StoryType;
import java.time.Instant;
import java.util.UUID;

/**
 * A start event, task or end event on the canvas; version guards edits from two tabs.
 * Table: workflows.nodes. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Node(
        UUID id,
        UUID workflowId,
        UUID laneId,
        NodeKind kind,
        String title,
        String description,
        TaskType taskType,
        String epicKey,
        String storyKey,
        StoryType storyType,
        int posX,
        int posY,
        int version,
        Instant createdAt,
        Instant updatedAt) {
}
