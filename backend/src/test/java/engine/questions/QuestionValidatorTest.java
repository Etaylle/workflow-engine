package engine.questions;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.InvalidAiOutputException;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionValidatorTest {

    private static QuestionDraft one(String type, List<String> options, List<String> proposal) {
        return new QuestionDraft(List.of(new QuestionDraft.Item("feature", type, "Q?", options, proposal, "why")));
    }

    @Test
    void acceptsTheCanteenExample() {
        assertThatCode(() -> QuestionValidator.validate(StubQuestionWriter.canteen())).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyAndTooMany() {
        assertThatThrownBy(() -> QuestionValidator.validate(new QuestionDraft(List.of())))
                .isInstanceOf(InvalidAiOutputException.class).hasMessageContaining("no questions");
        QuestionDraft.Item item = StubQuestionWriter.canteen().questions().get(0);
        assertThatThrownBy(() -> QuestionValidator.validate(
                new QuestionDraft(java.util.Collections.nCopies(9, item))))
                .hasMessageContaining("at most 8");
    }

    @Test
    void rejectsBadChoiceShapes() {
        assertThatThrownBy(() -> QuestionValidator.validate(one("single_choice", List.of("a"), List.of("a"))))
                .hasMessageContaining("2 to 6");
        assertThatThrownBy(() -> QuestionValidator.validate(one("single_choice", List.of("a", "b"), List.of("a", "b"))))
                .hasMessageContaining("exactly one");
        assertThatThrownBy(() -> QuestionValidator.validate(one("multiple_choice", List.of("a", "b"), List.of())))
                .hasMessageContaining("proposal");
        assertThatThrownBy(() -> QuestionValidator.validate(one("text", List.of("a"), List.of("x"))))
                .hasMessageContaining("text question");
        assertThatThrownBy(() -> QuestionValidator.validate(one("checkbox", List.of(), List.of("x"))))
                .hasMessageContaining("answerType");
    }
}
