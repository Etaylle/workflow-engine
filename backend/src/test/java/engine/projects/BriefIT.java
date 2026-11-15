package engine.projects;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.ScriptedAi.ScriptedBriefWriter;
import engine.TestHttp;
import engine.shared.InvalidAiOutputException;
import engine.shared.SessionKey;
import java.net.http.HttpResponse;
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

/** The brief through HTTP and the real PostgreSQL; Claude is replaced by a scripted writer. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedAi.class)
class BriefIT {

    @Autowired private ScriptedBriefWriter writer;
    @Autowired private SessionKey sessionKey;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void reset() {
        writer.reset();
        sessionKey.setAnthropic("sk-test");
        http = new TestHttp(port);
    }

    @Test
    void briefIsWrittenStoredAndSurvivesReload() throws Exception {
        UUID id = http.createProject("A canteen app");
        assertThat(http.call("GET", "/api/projects/" + id + "/brief", null).statusCode()).isEqualTo(404);

        HttpResponse<String> response = http.call("POST", "/api/projects/" + id + "/brief", null);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode again = http.json("GET", "/api/projects/" + id + "/brief", null);
        assertThat(again.get("goal").asString()).contains("order a meal");
        assertThat(again.get("actors")).hasSize(2);
        assertThat(http.statusOf(id)).isEqualTo("draft");
    }

    @Test
    void aConsoleBriefProposesTheConsoleTemplateAndTheClientCanChangeIt() throws Exception {
        UUID id = http.createProject("Make an app that opens a console and shows a duck");
        assertThat(http.json("GET", "/api/projects/" + id, null).get("templateId").asString())
                .isEqualTo("sveltekit-sqlite");
        writer.queue.add(new Brief("Show a duck in the console", List.of("User"), List.of("Duck"),
                List.of(), "console"));

        http.call("POST", "/api/projects/" + id + "/brief", null);

        assertThat(http.json("GET", "/api/projects/" + id, null).get("templateId").asString())
                .isEqualTo("node-console");
        assertThat(http.json("PATCH", "/api/projects/" + id, "{\"templateId\":\"java-svelte-postgres\"}")
                .get("templateId").asString()).isEqualTo("java-svelte-postgres");
        assertThat(http.call("PATCH", "/api/projects/" + id, "{\"templateId\":\"cobol\"}").statusCode())
                .isEqualTo(400);
    }

    @Test
    void invalidBriefIsRetriedOnceWithTheReason() throws Exception {
        UUID id = http.createProject("A canteen app");
        writer.queue.add(new Brief(" ", List.of(), List.of(), List.of(), "web"));

        assertThat(http.call("POST", "/api/projects/" + id + "/brief", null).statusCode()).isEqualTo(200);

        assertThat(writer.corrections).hasSize(2);
        assertThat(writer.corrections.get(1)).contains("no goal");
    }

    @Test
    void briefInvalidTwiceAnswers502AndStoresNothing() throws Exception {
        UUID id = http.createProject("A canteen app");
        writer.queue.add(new InvalidAiOutputException("first"));
        writer.queue.add(new InvalidAiOutputException("second"));

        assertThat(http.call("POST", "/api/projects/" + id + "/brief", null).statusCode()).isEqualTo(502);
        assertThat(http.call("GET", "/api/projects/" + id + "/brief", null).statusCode()).isEqualTo(404);
    }

    @Test
    void missingKeyAnswers409AndUnknownProject404() throws Exception {
        UUID id = http.createProject("A canteen app");
        sessionKey.clearAll();

        assertThat(http.call("POST", "/api/projects/" + id + "/brief", null).statusCode()).isEqualTo(409);
        assertThat(http.call("POST", "/api/projects/" + UUID.randomUUID() + "/brief", null).statusCode())
                .isEqualTo(404);
    }
}
