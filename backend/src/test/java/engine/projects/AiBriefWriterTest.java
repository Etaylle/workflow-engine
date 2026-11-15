package engine.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.ClaudeAnswers;
import engine.shared.InvalidAiOutputException;
import org.junit.jupiter.api.Test;

class AiBriefWriterTest {

    private static final String ANSWER =
            """
            {"goal":"Order lunch in the company canteen without queueing.",
             "actors":["Employee","Canteen staff"],
             "domainTerms":["Menu","Order","Pickup slot"],
             "risks":["Peak load at noon"]}
            """;

    @Test
    void realisticAnswerIsReadAndValid() {
        Brief brief = ClaudeAnswers.read(Brief.class, ANSWER);

        Brief.validate(brief);
        assertThat(brief.actors()).containsExactly("Employee", "Canteen staff");
        assertThat(brief.toText()).contains("Goal: Order lunch");
    }

    @Test
    void emptyListsAreValidButNoGoalIsNot() {
        Brief empty = ClaudeAnswers.read(Brief.class,
                "{\"goal\":\"g\",\"actors\":[\"a\"],\"domainTerms\":[],\"risks\":[]}");
        Brief.validate(empty);

        Brief noGoal = ClaudeAnswers.read(Brief.class,
                "{\"goal\":\" \",\"actors\":[\"a\"],\"domainTerms\":[],\"risks\":[]}");
        assertThatThrownBy(() -> Brief.validate(noGoal)).isInstanceOf(InvalidAiOutputException.class);
    }
}
