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
             "risks":["Peak load at noon"],
             "softwareKind":"web"}
            """;

    @Test
    void realisticAnswerIsReadAndValid() {
        Brief brief = ClaudeAnswers.read(Brief.class, ANSWER);

        Brief.validate(brief);
        assertThat(brief.actors()).containsExactly("Employee", "Canteen staff");
        assertThat(brief.toText()).contains("Goal: Order lunch");
    }

    private static final String DUCK_ANSWER =
            """
            {"goal":"Open a console window and show an ASCII duck, then end.",
             "actors":["User"],
             "domainTerms":["Console","Duck"],
             "risks":["Which operating system opens the console"],
             "softwareKind":"console"}
            """;

    @Test
    void aConsoleWishIsRecognisedAndTheBriefTextKeepsTheKind() {
        Brief brief = ClaudeAnswers.read(Brief.class, DUCK_ANSWER);

        Brief.validate(brief);
        assertThat(brief.softwareKind()).isEqualTo("console");
        assertThat(brief.toText()).contains("Kind of software: console program").contains("no web pages");
    }

    @Test
    void anUnknownOrMissingKindIsInvalid() {
        Brief unknown = ClaudeAnswers.read(Brief.class,
                "{\"goal\":\"g\",\"actors\":[\"a\"],\"domainTerms\":[],\"risks\":[],\"softwareKind\":\"desktop\"}");
        Brief missing = ClaudeAnswers.read(Brief.class,
                "{\"goal\":\"g\",\"actors\":[\"a\"],\"domainTerms\":[],\"risks\":[]}");

        assertThatThrownBy(() -> Brief.validate(unknown)).isInstanceOf(InvalidAiOutputException.class);
        assertThatThrownBy(() -> Brief.validate(missing)).isInstanceOf(InvalidAiOutputException.class);
    }

    @Test
    void emptyListsAreValidButNoGoalIsNot() {
        Brief empty = ClaudeAnswers.read(Brief.class,
                "{\"goal\":\"g\",\"actors\":[\"a\"],\"domainTerms\":[],\"risks\":[],\"softwareKind\":\"web\"}");
        Brief.validate(empty);

        Brief noGoal = ClaudeAnswers.read(Brief.class,
                "{\"goal\":\" \",\"actors\":[\"a\"],\"domainTerms\":[],\"risks\":[],\"softwareKind\":\"web\"}");
        assertThatThrownBy(() -> Brief.validate(noGoal)).isInstanceOf(InvalidAiOutputException.class);
    }
}
