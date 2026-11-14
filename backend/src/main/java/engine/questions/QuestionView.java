package engine.questions;

import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** A question as the API shows it; proposal and answer value are a string or a list of strings. */
public record QuestionView(
        UUID id,
        int seq,
        String category,
        String answerType,
        String text,
        List<String> options,
        JsonNode proposal,
        String explanation,
        Answer answer) {

    /** The answer given: the client's value, or the proposal accepted (decidedBy ai). */
    public record Answer(JsonNode value, String decidedBy) {
    }
}
