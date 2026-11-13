package engine.execution.domain;

/** One short answer the client can pick when human action is required (human_questions.options). */
public record AnswerOption(String key, String label, String hint, boolean recommended) {
}
