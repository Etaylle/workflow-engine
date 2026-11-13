package engine.questions.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The answer of the client, or the AI default when the client lets the AI decide.
 * Table: questions.answers. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Answer(
        UUID id,
        UUID questionId,
        List<String> value,
        DecidedBy decidedBy,
        Instant createdAt,
        Instant updatedAt) {
}
