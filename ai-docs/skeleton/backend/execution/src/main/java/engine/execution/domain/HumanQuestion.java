package engine.execution.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Human action required: question with short answer options.
 * Table: execution.human_questions. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record HumanQuestion(
        UUID id,
        UUID taskRunId,
        UUID runErrorId,
        String question,
        String expected,
        String actual,
        List<String> requirementRefs,
        String attemptsSummary,
        String cause,
        List<AnswerOption> options,
        HumanQuestionStatus status,
        String chosenOption,
        String answerText,
        Instant answeredAt,
        Instant createdAt,
        Instant updatedAt) {
}
