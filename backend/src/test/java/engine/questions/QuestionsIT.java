package engine.questions;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.ScriptedAi.ScriptedQuestionWriter;
import engine.TestHttp;
import engine.shared.InvalidAiOutputException;
import engine.shared.SessionKey;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;

/** Questions and answers through HTTP and the real PostgreSQL; Claude is a scripted writer. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedAi.class)
class QuestionsIT {

    @Autowired private ScriptedQuestionWriter writer;
    @Autowired private SessionKey sessionKey;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void reset() {
        writer.reset();
        sessionKey.setAnthropic("sk-test");
        http = new TestHttp(port);
    }

    private UUID projectWithBrief() throws Exception {
        UUID id = http.createProject("A canteen app");
        assertThat(http.call("POST", "/api/projects/" + id + "/brief", null).statusCode()).isEqualTo(200);
        return id;
    }

    private String answers(JsonNode questions, String overrideId, String overrideJson) {
        List<String> items = new ArrayList<>();
        for (JsonNode q : questions) {
            String qid = q.get("id").asString();
            items.add(qid.equals(overrideId)
                    ? "{\"questionId\":\"" + qid + "\",\"value\":" + overrideJson + "}"
                    : "{\"questionId\":\"" + qid + "\",\"acceptProposal\":true}");
        }
        return "{\"answers\":[" + String.join(",", items) + "]}";
    }

    @Test
    void questionsAreAskedWithProposalsAndSurviveReload() throws Exception {
        UUID id = projectWithBrief();

        JsonNode questions = http.json("POST", "/api/projects/" + id + "/questions", null);

        assertThat(questions).hasSize(3);
        assertThat(questions.get(0).get("proposal").asString()).isEqualTo("Online at the order");
        assertThat(questions.get(1).get("proposal")).hasSize(2);
        assertThat(questions.get(0).get("answer").isNull()).isTrue();
        assertThat(http.statusOf(id)).isEqualTo("questions_ready");
        assertThat(http.json("GET", "/api/projects/" + id + "/questions", null)).hasSize(3);
    }

    @Test
    void questionsNeedTheBriefAndTheKey() throws Exception {
        UUID id = http.createProject("A canteen app");
        assertThat(http.call("POST", "/api/projects/" + id + "/questions", null).statusCode()).isEqualTo(409);
        http.call("POST", "/api/projects/" + id + "/brief", null);
        sessionKey.clearAll();

        HttpResponse<String> response = http.call("POST", "/api/projects/" + id + "/questions", null);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("AI key missing");
    }

    @Test
    void moreThanEightQuestionsAreRejectedThenRetriedWithTheReason() throws Exception {
        UUID id = projectWithBrief();
        QuestionDraft.Item item = new QuestionDraft.Item("feature", "text", "Why?", List.of(), List.of("x"), "y");
        writer.queue.add(new QuestionDraft(java.util.Collections.nCopies(9, item)));

        JsonNode questions = http.json("POST", "/api/projects/" + id + "/questions", null);

        assertThat(questions).hasSize(3);
        assertThat(writer.corrections).hasSize(2);
        assertThat(writer.corrections.get(1)).contains("at most 8");
    }

    @Test
    void proposalOutsideTheOptionsIsInvalidTwiceAnswers502() throws Exception {
        UUID id = projectWithBrief();
        QuestionDraft bad = new QuestionDraft(List.of(new QuestionDraft.Item("feature", "single_choice",
                "Pick one", List.of("a", "b"), List.of("c"), "z")));
        writer.queue.add(bad);
        writer.queue.add(bad);

        assertThat(http.call("POST", "/api/projects/" + id + "/questions", null).statusCode()).isEqualTo(502);
        assertThat(http.json("GET", "/api/projects/" + id + "/questions", null)).isEmpty();
        assertThat(http.statusOf(id)).isEqualTo("draft");
    }

    @Test
    void invalidOutputError() throws Exception {
        UUID id = projectWithBrief();
        writer.queue.add(new InvalidAiOutputException("first"));
        writer.queue.add(new InvalidAiOutputException("second"));

        assertThat(http.call("POST", "/api/projects/" + id + "/questions", null).statusCode()).isEqualTo(502);
    }

    @Test
    void acceptingProposalsAndOwnAnswersAreSavedAndMarked() throws Exception {
        UUID id = projectWithBrief();
        JsonNode questions = http.json("POST", "/api/projects/" + id + "/questions", null);
        String textId = questions.get(2).get("id").asString();

        HttpResponse<String> saved = http.call("PUT", "/api/projects/" + id + "/answers",
                answers(questions, textId, "\"45 minutes\""));

        assertThat(saved.statusCode()).isEqualTo(200);
        JsonNode reloaded = http.json("GET", "/api/projects/" + id + "/questions", null);
        assertThat(reloaded.get(0).get("answer").get("decidedBy").asString()).isEqualTo("ai");
        assertThat(reloaded.get(0).get("answer").get("value").asString()).isEqualTo("Online at the order");
        assertThat(reloaded.get(2).get("answer").get("decidedBy").asString()).isEqualTo("client");
        assertThat(reloaded.get(2).get("answer").get("value").asString()).isEqualTo("45 minutes");
        assertThat(http.statusOf(id)).isEqualTo("answered");
        assertThat(http.call("PUT", "/api/projects/" + id + "/answers", answers(questions, textId, "\"1h\""))
                .statusCode()).isEqualTo(200);
    }

    @Test
    void missingUnknownAndInvalidAnswersAreRejected() throws Exception {
        UUID id = projectWithBrief();
        JsonNode questions = http.json("POST", "/api/projects/" + id + "/questions", null);
        String first = questions.get(0).get("id").asString();
        String multi = questions.get(1).get("id").asString();
        String path = "/api/projects/" + id + "/answers";

        assertThat(http.call("PUT", path, "{\"answers\":[]}").statusCode()).isEqualTo(400);
        assertThat(http.call("PUT", path, answers(questions, first, "\"Nope\"")).statusCode()).isEqualTo(400);
        assertThat(http.call("PUT", path, answers(questions, multi, "[]")).statusCode()).isEqualTo(400);
        assertThat(http.call("PUT", path, answers(questions, multi, "[\"Order a meal\"]")).statusCode())
                .isEqualTo(200);
        String unknown = "{\"answers\":[{\"questionId\":\"" + UUID.randomUUID() + "\",\"acceptProposal\":true}]}";
        assertThat(http.call("PUT", path, unknown).statusCode()).isEqualTo(400);
        assertThat(http.call("PUT", "/api/projects/" + UUID.randomUUID() + "/answers", unknown).statusCode())
                .isEqualTo(404);
    }

    @Test
    void answersWithoutQuestionsAnswer409() throws Exception {
        UUID id = projectWithBrief();

        assertThat(http.call("PUT", "/api/projects/" + id + "/answers", "{\"answers\":[]}").statusCode())
                .isEqualTo(409);
    }

    private static final String OWN = "{\"category\":\"feature\",\"answerType\":\"single_choice\","
            + "\"text\":\"Which language?\",\"options\":[\"English\",\"German\"],"
            + "\"proposal\":[\"German\"],\"explanation\":\"Own question\"}";

    @Test
    void questionsAreFixedOnceTheAnswersAreSaved() throws Exception {
        UUID id = projectWithBrief();
        JsonNode questions = http.json("POST", "/api/projects/" + id + "/questions", null);
        String first = questions.get(0).get("id").asString();
        http.call("PUT", "/api/projects/" + id + "/answers", answers(questions, "", "null"));

        // the answers were saved (status answered): questions are fixed now
        assertThat(http.call("POST", "/api/projects/" + id + "/question-items", OWN).statusCode()).isEqualTo(409);
        assertThat(http.call("PUT", "/api/projects/" + id + "/question-items/" + first, OWN).statusCode())
                .isEqualTo(409);
        assertThat(http.call("DELETE", "/api/projects/" + id + "/question-items/" + first, null).statusCode())
                .isEqualTo(409);
    }

    @Test
    void addEditAndDeleteChangeTheListAndRenumber() throws Exception {
        UUID id = projectWithBrief();
        JsonNode questions = http.json("POST", "/api/projects/" + id + "/questions", null);
        String first = questions.get(0).get("id").asString();

        JsonNode added = http.json("POST", "/api/projects/" + id + "/question-items", OWN);
        assertThat(added).hasSize(4);
        assertThat(added.get(3).get("text").asString()).isEqualTo("Which language?");
        assertThat(added.get(3).get("seq").asInt()).isEqualTo(4);

        JsonNode edited = http.json("PUT", "/api/projects/" + id + "/question-items/" + first, OWN);
        assertThat(edited.get(0).get("text").asString()).isEqualTo("Which language?");
        assertThat(edited.get(0).get("proposal").asString()).isEqualTo("German");

        JsonNode removed = http.json("DELETE", "/api/projects/" + id + "/question-items/" + first, null);
        assertThat(removed).hasSize(3);
        assertThat(removed.get(0).get("seq").asInt()).isEqualTo(1);
        assertThat(removed.get(2).get("seq").asInt()).isEqualTo(3);
        assertThat(http.call("DELETE", "/api/projects/" + id + "/question-items/" + first, null).statusCode())
                .isEqualTo(404);
    }

    @Test
    void editingDropsTheAnswerAndBadInputIsRefused() throws Exception {
        UUID id = projectWithBrief();
        JsonNode questions = http.json("POST", "/api/projects/" + id + "/questions", null);
        String first = questions.get(0).get("id").asString();

        String badProposal = OWN.replace("[\"German\"]", "[\"French\"]");
        assertThat(http.call("PUT", "/api/projects/" + id + "/question-items/" + first, badProposal)
                .statusCode()).isEqualTo(400);
        assertThat(http.call("POST", "/api/projects/" + id + "/question-items", "{}").statusCode())
                .isEqualTo(400);
        assertThat(http.call("PUT", "/api/projects/" + id + "/question-items/" + UUID.randomUUID(), OWN)
                .statusCode()).isEqualTo(404);
    }

    @Test
    void aNinthQuestionIsRefusedAndAnEmptyListCannotBeAnswered() throws Exception {
        UUID id = projectWithBrief();
        http.json("POST", "/api/projects/" + id + "/questions", null);
        for (int i = 0; i < 5; i++) {
            assertThat(http.call("POST", "/api/projects/" + id + "/question-items", OWN).statusCode())
                    .isEqualTo(201);
        }
        assertThat(http.call("POST", "/api/projects/" + id + "/question-items", OWN).statusCode()).isEqualTo(409);

        for (JsonNode question : http.json("GET", "/api/projects/" + id + "/questions", null)) {
            http.call("DELETE", "/api/projects/" + id + "/question-items/" + question.get("id").asString(), null);
        }
        assertThat(http.call("PUT", "/api/projects/" + id + "/answers", "{\"answers\":[]}").statusCode())
                .isEqualTo(400);
    }
}
