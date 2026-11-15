package engine.workflows;

import static org.assertj.core.api.Assertions.assertThat;

import engine.shared.ClaudeAnswers;
import org.junit.jupiter.api.Test;

class AnthropicWorkflowWriterTest {

    private static final String ANSWER =
            """
            {"lanes":["Employee","System"],
             "nodes":[
              {"key":"s","lane":"Employee","kind":"start_event","title":"Hungry","description":"","taskType":""},
              {"key":"t1","lane":"System","kind":"task","title":"Show menu","description":"List meals","taskType":"software"},
              {"key":"e","lane":"Employee","kind":"end_event","title":"Fed","description":"","taskType":""}],
             "edges":[{"from":"s","to":"t1"},{"from":"t1","to":"e"}]}
            """;

    @Test
    void realisticAnswerIsReadAndValid() {
        WorkflowDraft draft = ClaudeAnswers.read(WorkflowDraft.class, ANSWER);

        WorkflowValidator.validate(draft);
        assertThat(draft.nodes()).hasSize(3);
    }

    @Test
    void stubDraftSurvivesTheJsonRoundTrip() throws Exception {
        WorkflowDraft canteen = StubWorkflowWriter.canteen();
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(canteen);

        assertThat(ClaudeAnswers.read(WorkflowDraft.class, json)).isEqualTo(canteen);
    }
}
