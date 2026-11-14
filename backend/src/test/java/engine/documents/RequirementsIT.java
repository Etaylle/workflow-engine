package engine.documents;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.ScriptedAi.ScriptedRequirementWriter;
import engine.TestHttp;
import engine.shared.SessionKey;
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

/** Requirements through HTTP and the real PostgreSQL; Claude is a scripted writer. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedAi.class)
class RequirementsIT {

    @Autowired private ScriptedRequirementWriter writer;
    @Autowired private SessionKey sessionKey;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void reset() {
        writer.reset();
        writer.materials.clear();
        sessionKey.set("sk-test");
        http = new TestHttp(port);
    }

    @Test
    void requirementsAreWrittenFromTheAnswersGroupedByEpicAndSurviveReload() throws Exception {
        UUID id = http.answeredProject("A canteen app");

        JsonNode view = http.json("POST", "/api/projects/" + id + "/requirements", null);

        assertThat(view.get("epics")).hasSize(2);
        assertThat(view.get("epics").get(0).get("key").asString()).isEqualTo("E-01");
        assertThat(view.get("epics").get(0).get("requirements").get(0).get("key").asString())
                .isEqualTo("FR-001");
        assertThat(writer.materials.get(0)).contains("How do students pay? -> Online at the order")
                .contains("Goal:");
        assertThat(http.statusOf(id)).isEqualTo("requirements_ready");
        JsonNode again = http.json("GET", "/api/projects/" + id + "/requirements", null);
        assertThat(again.get("epics").get(1).get("requirements")).hasSize(2);
    }

    @Test
    void writingAgainReplacesTheEarlierRequirements() throws Exception {
        UUID id = http.answeredProject("A canteen app");
        http.call("POST", "/api/projects/" + id + "/requirements", null);
        writer.queue.add(new RequirementDraft(List.of(new RequirementDraft.Epic("Only", "Only",
                List.of(new RequirementDraft.Item("quality", "could", "It is fast.", null))))));

        JsonNode view = http.json("POST", "/api/projects/" + id + "/requirements", null);

        assertThat(view.get("epics")).hasSize(1);
        assertThat(view.get("epics").get(0).get("requirements").get(0).get("key").asString())
                .isEqualTo("QR-001");
    }

    @Test
    void invalidRequirementsAreRetriedOnceWithTheReason() throws Exception {
        UUID id = http.answeredProject("A canteen app");
        writer.queue.add(new RequirementDraft(List.of(new RequirementDraft.Epic("A", "A",
                List.of(new RequirementDraft.Item("wish", "must", "x", null))))));

        assertThat(http.call("POST", "/api/projects/" + id + "/requirements", null).statusCode())
                .isEqualTo(200);
        assertThat(writer.corrections).hasSize(2);
        assertThat(writer.corrections.get(1)).contains("Requirement type 'wish'");
    }

    @Test
    void invalidTwiceAnswers502AndKeepsTheStatus() throws Exception {
        UUID id = http.answeredProject("A canteen app");
        RequirementDraft empty = new RequirementDraft(List.of());
        writer.queue.add(empty);
        writer.queue.add(empty);

        assertThat(http.call("POST", "/api/projects/" + id + "/requirements", null).statusCode())
                .isEqualTo(502);
        assertThat(http.statusOf(id)).isEqualTo("answered");
    }

    @Test
    void notAnsweredYetKeyMissingAndUnknownProject() throws Exception {
        UUID fresh = http.createProject("A canteen app");
        assertThat(http.call("POST", "/api/projects/" + fresh + "/requirements", null).statusCode())
                .isEqualTo(409);
        UUID id = http.answeredProject("A canteen app");
        sessionKey.clear();
        assertThat(http.call("POST", "/api/projects/" + id + "/requirements", null).body())
                .contains("Anthropic key missing");
        assertThat(http.call("GET", "/api/projects/" + UUID.randomUUID() + "/requirements", null).statusCode())
                .isEqualTo(404);
        assertThat(http.json("GET", "/api/projects/" + fresh + "/requirements", null).get("epics")).isEmpty();
    }
}
