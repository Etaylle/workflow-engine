package engine.questions.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Sources the questions agent used (insert-only).
 * Table: questions.research_sources. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record ResearchSource(
        UUID id,
        UUID projectId,
        String url,
        String title,
        String summary,
        Instant createdAt) {
}
