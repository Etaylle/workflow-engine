package engine.questions;

import engine.shared.InvalidAiOutputException;
import java.util.List;
import java.util.Set;

/** Checks the questions Claude wrote; the message of a failure is sent back on the retry. */
final class QuestionValidator {

    static final int MAX_QUESTIONS = 8;
    private static final Set<String> CATEGORIES = Set.of("requirement", "feature");
    private static final Set<String> TYPES = Set.of("single_choice", "multiple_choice", "text");

    private QuestionValidator() {
    }

    static void validate(QuestionDraft draft) {
        if (draft == null || draft.questions() == null || draft.questions().isEmpty()) {
            throw new InvalidAiOutputException("There are no questions.");
        }
        if (draft.questions().size() > MAX_QUESTIONS) {
            throw new InvalidAiOutputException(
                    "There are " + draft.questions().size() + " questions; at most 8 are allowed.");
        }
        for (int i = 0; i < draft.questions().size(); i++) {
            item(draft.questions().get(i), "Question " + (i + 1));
        }
    }

    /** Checks one question written or edited by the client. */
    static void validateItem(QuestionDraft.Item item) {
        item(item, "The question");
    }

    private static void item(QuestionDraft.Item item, String label) {
        if (item.text() == null || item.text().isBlank()) {
            throw new InvalidAiOutputException(label + " has no text.");
        }
        if (!CATEGORIES.contains(item.category())) {
            throw new InvalidAiOutputException(label + " has category '" + item.category()
                    + "'; use requirement or feature.");
        }
        if (!TYPES.contains(item.answerType())) {
            throw new InvalidAiOutputException(label + " has answerType '" + item.answerType()
                    + "'; use single_choice, multiple_choice or text.");
        }
        List<String> options = item.options() == null ? List.of() : item.options();
        List<String> proposal = item.proposal() == null ? List.of() : item.proposal();
        if ("text".equals(item.answerType())) {
            if (!options.isEmpty() || proposal.size() != 1 || proposal.get(0).isBlank()) {
                throw new InvalidAiOutputException(
                        label + " is a text question: no options and exactly one proposal text.");
            }
            return;
        }
        if (options.size() < 2 || options.size() > 6 || options.stream().anyMatch(String::isBlank)) {
            throw new InvalidAiOutputException(label + " needs 2 to 6 non-empty options.");
        }
        boolean single = "single_choice".equals(item.answerType());
        if (proposal.isEmpty() || (single && proposal.size() != 1) || !options.containsAll(proposal)) {
            throw new InvalidAiOutputException(label + " needs a proposal taken from its options"
                    + (single ? " (exactly one)." : "."));
        }
    }
}
