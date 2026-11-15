package engine.questions;

import static org.assertj.core.api.Assertions.assertThat;

import engine.shared.ClaudeAnswers;
import org.junit.jupiter.api.Test;

class AnthropicQuestionWriterTest {

    private static final String ANSWER =
            """
            {"questions":[
              {"category":"requirement","answerType":"single_choice","text":"How is lunch paid?",
               "options":["Payroll deduction","Card","Cash"],"proposal":["Payroll deduction"],
               "explanation":"Common in company canteens."},
              {"category":"feature","answerType":"multiple_choice","text":"Which notices?",
               "options":["Mail","Push"],"proposal":["Mail","Push"],"explanation":"Both are cheap."},
              {"category":"feature","answerType":"text","text":"Opening hours?",
               "options":[],"proposal":["11:00 to 14:00"],"explanation":"Typical lunch window."}]}
            """;

    @Test
    void realisticAnswerIsReadAndValid() {
        QuestionDraft draft = ClaudeAnswers.read(QuestionDraft.class, ANSWER);

        QuestionValidator.validate(draft);
        assertThat(draft.questions()).hasSize(3);
        assertThat(draft.questions().get(2).options()).isEmpty();
    }

    @Test
    void stubDraftSurvivesTheJsonRoundTrip() throws Exception {
        QuestionDraft canteen = StubQuestionWriter.canteen();
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(canteen);

        assertThat(ClaudeAnswers.read(QuestionDraft.class, json)).isEqualTo(canteen);
    }
}
