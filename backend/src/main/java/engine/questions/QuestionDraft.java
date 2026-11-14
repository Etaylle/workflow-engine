package engine.questions;

import java.util.List;

/**
 * The questions as Claude writes them (structured output by class). Category is requirement or
 * feature; answerType is single_choice, multiple_choice or text. The proposal is the AI's answer:
 * one element for single_choice and text, one or more of the options for multiple_choice.
 */
public record QuestionDraft(List<Item> questions) {

    /** One question with its options (empty for text), the proposal and a short reason for it. */
    public record Item(
            String category,
            String answerType,
            String text,
            List<String> options,
            List<String> proposal,
            String explanation) {
    }
}
