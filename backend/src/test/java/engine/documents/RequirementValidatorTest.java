package engine.documents;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.InvalidAiOutputException;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementValidatorTest {

    private static RequirementDraft one(RequirementDraft.Item item) {
        return new RequirementDraft(List.of(new RequirementDraft.Epic("E", "D", List.of(item))));
    }

    @Test
    void acceptsTheCanteenExample() {
        assertThatCode(() -> RequirementValidator.validate(
                new StubRequirementWriter().write("k", "w", "m", null))).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyEpicsAndEmptyEpic() {
        assertThatThrownBy(() -> RequirementValidator.validate(new RequirementDraft(List.of())))
                .isInstanceOf(InvalidAiOutputException.class).hasMessageContaining("no epics");
        assertThatThrownBy(() -> RequirementValidator.validate(
                new RequirementDraft(List.of(new RequirementDraft.Epic("E", "D", List.of())))))
                .hasMessageContaining("no requirements");
    }

    @Test
    void rejectsUnknownTypePriorityAndBlankText() {
        assertThatThrownBy(() -> RequirementValidator.validate(one(new RequirementDraft.Item("x", "must", "t", null))))
                .hasMessageContaining("type");
        assertThatThrownBy(() -> RequirementValidator.validate(one(new RequirementDraft.Item("quality", "high", "t", null))))
                .hasMessageContaining("priority");
        assertThatThrownBy(() -> RequirementValidator.validate(one(new RequirementDraft.Item("quality", "must", " ", null))))
                .hasMessageContaining("no text");
    }

    @Test
    void rejectsMoreThanFortyRequirements() {
        RequirementDraft.Item item = new RequirementDraft.Item("quality", "must", "t", null);
        assertThatThrownBy(() -> RequirementValidator.validate(new RequirementDraft(List.of(
                new RequirementDraft.Epic("E", "D", java.util.Collections.nCopies(41, item))))))
                .hasMessageContaining("at most 40");
    }
}
