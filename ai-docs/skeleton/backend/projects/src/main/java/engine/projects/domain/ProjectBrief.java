package engine.projects.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The brief written by the prompt agent.
 * Table: projects.project_briefs. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record ProjectBrief(
        UUID projectId,
        String goal,
        List<String> actors,
        List<String> domainTerms,
        List<String> risks,
        Instant createdAt,
        Instant updatedAt) {
}
