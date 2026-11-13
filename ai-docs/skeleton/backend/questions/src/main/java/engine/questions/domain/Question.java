package engine.questions.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One question of the questions agent.
 * Table: questions.questions. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Question(
        UUID id,
        UUID projectId,
        int seq,
        QuestionCategory category,
        AnswerType answerType,
        String text,
        List<String> options,
        String aiDefault,
        String explanation,
        Instant createdAt,
        Instant updatedAt) {
}
