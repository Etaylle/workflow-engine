package engine.questions;

import java.util.List;

/** Offline writer for tests and ENGINE_AI_STUB=true: always the canteen example. */
public class StubQuestionWriter implements QuestionWriter {

    @Override
    public QuestionDraft write(String apiKey, String wish, String brief, String correction) {
        return canteen();
    }

    public static QuestionDraft canteen() {
        return new QuestionDraft(List.of(
                new QuestionDraft.Item("requirement", "single_choice", "How do students pay?",
                        List.of("Online at the order", "At pick-up", "Prepaid card"),
                        List.of("Online at the order"), "Paying at the order avoids queues at the counter."),
                new QuestionDraft.Item("feature", "multiple_choice", "What can a student do?",
                        List.of("Order a meal", "See the menu", "Cancel an order", "Rate a meal"),
                        List.of("Order a meal", "See the menu"), "These two are the core of the wish."),
                new QuestionDraft.Item("requirement", "text", "How long is the pick-up window?",
                        List.of(), List.of("30 minutes"), "Meals stay fresh for about half an hour.")));
    }
}
