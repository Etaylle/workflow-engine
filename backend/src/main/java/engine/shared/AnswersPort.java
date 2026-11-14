package engine.shared;

import java.util.List;
import java.util.UUID;

/** What other domains may read of the domain "questions". */
public interface AnswersPort {

    /** One question with the answer given (by the client or by the AI proposal). */
    record AnswerLine(String question, String answer, String decidedBy) {
    }

    /** The answers of the project in the order of the questions; empty when not answered yet. */
    List<AnswerLine> answersOf(UUID projectId);
}
