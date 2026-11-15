package engine.documents;

import static org.assertj.core.api.Assertions.assertThat;

import engine.shared.ClaudeAnswers;
import org.junit.jupiter.api.Test;

class AnthropicRequirementWriterTest {

    private static final String ANSWER =
            """
            {"epics":[{"name":"Ordering","domainName":"orders","requirements":[
              {"type":"functional","priority":"must","text":"An employee can order a meal.","source":"wish"},
              {"type":"quality","priority":"should","text":"A page loads within 2 seconds.","source":"answer: speed"}]}]}
            """;

    @Test
    void realisticAnswerIsReadAndValid() {
        RequirementDraft draft = ClaudeAnswers.read(RequirementDraft.class, ANSWER);

        RequirementValidator.validate(draft);
        assertThat(draft.epics().get(0).requirements()).hasSize(2);
    }

    @Test
    void stubDraftSurvivesTheJsonRoundTrip() throws Exception {
        RequirementDraft canteen = new StubRequirementWriter().write("k", "w", "m", null);
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(canteen);

        RequirementDraft read = ClaudeAnswers.read(RequirementDraft.class, json);

        assertThat(read).isEqualTo(canteen);
        RequirementValidator.validate(read);
    }
}
