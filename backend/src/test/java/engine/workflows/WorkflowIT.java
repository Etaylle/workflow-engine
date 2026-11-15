package engine.workflows;

import engine.shared.InvalidAiOutputException;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.ScriptedAi.ScriptedWorkflowWriter;
import engine.TestHttp;
import engine.shared.ApiProblem;
import engine.shared.SessionKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The slice through HTTP and the real PostgreSQL; Claude is replaced by a scripted writer. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedAi.class)
class WorkflowIT {

        private static final JsonMapper JSON = TestHttp.JSON;

    @Autowired private ScriptedWorkflowWriter writer;
    @Autowired private SessionKey sessionKey;
    @Autowired private JdbcClient jdbc;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void reset() {
        writer.reset();
        writer.materials.clear();
        sessionKey.setAnthropic("sk-test");
        http = new TestHttp(port);
    }
    @Test
    void projectIsCreatedListedAndReopened() throws Exception {
        UUID id = createProject("Canteen ordering\nmore text");

        JsonNode one = JSON.readTree(call("GET", "/api/projects/" + id, null).body());
        assertThat(one.get("name").asString()).isEqualTo("Canteen ordering");
        assertThat(one.get("status").asString()).isEqualTo("draft");
        assertThat(call("GET", "/api/projects", null).body()).contains(id.toString());
    }

    @Test
    void emptyWishIsRejectedAndNoKeyIsStored() throws Exception {
        HttpResponse<String> response = call("POST", "/api/projects", "{\"wish\":\"   \",\"apiKey\":\"sk-x\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("content-type").orElse("")).contains("problem+json");
        assertThat(sessionKey.anthropicKey()).contains("sk-test");
        assertThat(call("GET", "/api/projects/" + UUID.randomUUID(), null).statusCode()).isEqualTo(404);
    }

    @Test
    void workflowIsGeneratedStoredAndSurvivesReload() throws Exception {
        UUID id = plannedProject("A canteen app");

        HttpResponse<String> generated = call("POST", "/api/projects/" + id + "/workflow", null);

        assertThat(generated.statusCode()).isEqualTo(200);
        JsonNode view = JSON.readTree(generated.body());
        assertThat(view.get("status").asString()).isEqualTo("valid");
        assertThat(view.get("nodes")).hasSize(6);
        assertThat(view.get("edges")).hasSize(5);
        assertThat(view.get("lanes")).hasSize(2);
        JsonNode again = JSON.readTree(call("GET", "/api/projects/" + id + "/workflow", null).body());
        assertThat(again.get("nodes")).hasSize(6);
        assertThat(JSON.readTree(call("GET", "/api/projects/" + id, null).body()).get("status").asString())
                .isEqualTo("workflow_drafted");
    }

    @Test
    void missingKeyAnswers409() throws Exception {
        UUID id = plannedProject("A canteen app");
        sessionKey.clearAll();

        HttpResponse<String> response = call("POST", "/api/projects/" + id + "/workflow", null);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("AI key missing");
    }

    @Test
    void invalidOutputIsRetriedOnceWithTheReason() throws Exception {
        UUID id = plannedProject("A canteen app");
        writer.queue.add(new WorkflowDraft(List.of(), List.of(), List.of()));

        HttpResponse<String> response = call("POST", "/api/projects/" + id + "/workflow", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(writer.corrections).hasSize(2);
        assertThat(writer.corrections.get(0)).isNull();
        assertThat(writer.corrections.get(1)).contains("lanes");
    }

    @Test
    void outputInvalidTwiceAnswers502AndStoresNothing() throws Exception {
        UUID id = plannedProject("A canteen app");
        writer.queue.add(new InvalidAiOutputException("first"));
        writer.queue.add(new InvalidAiOutputException("second"));

        HttpResponse<String> response = call("POST", "/api/projects/" + id + "/workflow", null);

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(call("GET", "/api/projects/" + id + "/workflow", null).statusCode()).isEqualTo(404);
    }

    @Test
    void rateLimitAndRejectedKeyKeepTheirStatus() throws Exception {
        UUID id = plannedProject("A canteen app");
        writer.queue.add(new ApiProblem(429, "Rate limit reached", "slow down"));
        assertThat(call("POST", "/api/projects/" + id + "/workflow", null).statusCode()).isEqualTo(429);
        writer.queue.add(new ApiProblem(401, "Anthropic key rejected", "bad key"));
        assertThat(call("POST", "/api/projects/" + id + "/workflow", null).statusCode()).isEqualTo(401);
    }

    @Test
    void confirmedContractNeverChangesAndANewConfirmationMakesVersionTwo() throws Exception {
        UUID id = plannedProject("A canteen app");
        call("POST", "/api/projects/" + id + "/workflow", null);
        assertThat(call("POST", "/api/projects/" + id + "/workflow/confirm", null).statusCode()).isEqualTo(201);
        String snapshotV1 = snapshot(id, 1);
        assertThat(call("POST", "/api/projects/" + id + "/workflow/confirm", null).statusCode()).isEqualTo(409);

        WorkflowDraft smaller = new WorkflowDraft(
                List.of("Student"),
                List.of(new WorkflowDraft.Node("s", "Student", "start_event", "Start", "", ""),
                        new WorkflowDraft.Node("e", "Student", "end_event", "End", "", "")),
                List.of(new WorkflowDraft.Edge("s", "e")));
        writer.queue.add(smaller);
        call("POST", "/api/projects/" + id + "/workflow", null);
        HttpResponse<String> second = call("POST", "/api/projects/" + id + "/workflow/confirm", null);

        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(JSON.readTree(second.body()).get("version").asInt()).isEqualTo(2);
        assertThat(snapshot(id, 1)).isEqualTo(snapshotV1);
        assertThat(snapshot(id, 2)).isNotEqualTo(snapshotV1);
        assertThat(JSON.readTree(call("GET", "/api/projects/" + id + "/contracts", null).body())).hasSize(2);
    }

    @Test
    void databaseRefusesToChangeOrDeleteAContract() throws Exception {
        UUID id = plannedProject("A canteen app");
        call("POST", "/api/projects/" + id + "/workflow", null);
        call("POST", "/api/projects/" + id + "/workflow/confirm", null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.sql(
                        "update workflows.contracts set snapshot = '{}'::jsonb where project_id = :p")
                .param("p", id).update()).hasMessageContaining("never changes");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.sql(
                        "delete from workflows.contracts where project_id = :p")
                .param("p", id).update()).hasMessageContaining("never changes");
    }

    @Test
    void workflowNeedsRequirementsAndGetsThePlanningMaterial() throws Exception {
        UUID fresh = createProject("A canteen app");
        HttpResponse<String> refused = call("POST", "/api/projects/" + fresh + "/workflow", null);
        assertThat(refused.statusCode()).isEqualTo(409);
        assertThat(refused.body()).contains("Requirements missing");
        UUID id = plannedProject("A canteen app");

        call("POST", "/api/projects/" + id + "/workflow", null);

        assertThat(writer.materials.get(0)).contains("Online at the order").contains("FR-001")
                .contains("Goal:");
    }

    @Test
    void contractSnapshotHoldsAnswersAndRequirements() throws Exception {
        UUID id = plannedProject("A canteen app");
        call("POST", "/api/projects/" + id + "/workflow", null);
        call("POST", "/api/projects/" + id + "/workflow/confirm", null);

        JsonNode snapshot = JSON.readTree(snapshot(id, 1));

        assertThat(snapshot.get("answers")).hasSize(3);
        assertThat(snapshot.get("answers").get(0).get("answer").asString()).isEqualTo("Online at the order");
        assertThat(snapshot.get("requirements")).hasSize(5);
        assertThat(snapshot.get("brief").asString()).contains("Goal:");
        assertThat(snapshot.get("workflow").get("nodes")).hasSize(6);
    }

    @Test
    void confirmWithoutWorkflowAnswers409() throws Exception {
        UUID id = plannedProject("A canteen app");

        assertThat(call("POST", "/api/projects/" + id + "/workflow/confirm", null).statusCode()).isEqualTo(409);
    }

    private static final String EDITED = "{\"lanes\":[\"Student\"],\"nodes\":["
            + "{\"key\":\"s\",\"lane\":\"Student\",\"kind\":\"start_event\",\"title\":\"Hungry\"},"
            + "{\"key\":\"t\",\"lane\":\"Student\",\"kind\":\"task\",\"title\":\"Order\","
            + "\"description\":\"Choose lunch from the weekly menu\",\"taskType\":\"software\"},"
            + "{\"key\":\"e\",\"lane\":\"Student\",\"kind\":\"end_event\",\"title\":\"Fed\"}],"
            + "\"edges\":[{\"from\":\"s\",\"to\":\"t\"},{\"from\":\"t\",\"to\":\"e\"}]}";

    @Test
    void editedWorkflowIsStoredAndAConfirmedOneMakesVersionTwo() throws Exception {
        UUID id = plannedProject("A canteen app");
        assertThat(call("PUT", "/api/projects/" + id + "/workflow", EDITED).statusCode()).isEqualTo(409);
        call("POST", "/api/projects/" + id + "/workflow", null);
        call("POST", "/api/projects/" + id + "/workflow/confirm", null);

        HttpResponse<String> edited = call("PUT", "/api/projects/" + id + "/workflow", EDITED);

        assertThat(edited.statusCode()).isEqualTo(200);
        JsonNode view = JSON.readTree(edited.body());
        assertThat(view.get("status").asString()).isEqualTo("valid");
        assertThat(view.get("nodes")).hasSize(3);
        assertThat(view.get("nodes").get(1).get("description").asString()).isEqualTo("Choose lunch from the weekly menu");
        assertThat(view.get("confirmedContractVersion").asInt()).isEqualTo(1);
        assertThat(http.statusOf(id)).isEqualTo("workflow_drafted");
        assertThat(call("POST", "/api/projects/" + id + "/workflow/confirm", null).statusCode()).isEqualTo(201);
        assertThat(JSON.readTree(call("GET", "/api/projects/" + id + "/contracts", null).body())).hasSize(2);
        assertThat(snapshot(id, 1)).contains("Order a meal").doesNotContain("Choose lunch from the weekly menu");
        assertThat(snapshot(id, 2)).contains("Choose lunch from the weekly menu");
    }

    @Test
    void editedWorkflowBreakingARuleAnswers400AndKeepsTheStoredOne() throws Exception {
        UUID id = plannedProject("A canteen app");
        call("POST", "/api/projects/" + id + "/workflow", null);

        HttpResponse<String> noEnd = call("PUT", "/api/projects/" + id + "/workflow",
                EDITED.replace("\"kind\":\"end_event\"", "\"kind\":\"task\",\"taskType\":\"manual\""));
        HttpResponse<String> unknownLane = call("PUT", "/api/projects/" + id + "/workflow",
                EDITED.replace("\"lane\":\"Student\",\"kind\":\"end_event\"", "\"lane\":\"Nobody\",\"kind\":\"end_event\""));

        assertThat(noEnd.statusCode()).isEqualTo(400);
        assertThat(noEnd.body()).contains("end_event");
        assertThat(unknownLane.statusCode()).isEqualTo(400);
        assertThat(JSON.readTree(call("GET", "/api/projects/" + id + "/workflow", null).body()).get("nodes"))
                .hasSize(6);
    }

    private String snapshot(UUID projectId, int version) {
        return jdbc.sql("select snapshot::text from workflows.contracts"
                        + " where project_id = :p and contract_version = :v")
                .param("p", projectId).param("v", version).query(String.class).single();
    }

    private UUID plannedProject(String wish) throws Exception {
        UUID id = http.answeredProject(wish);
        assertThat(call("POST", "/api/projects/" + id + "/requirements", null).statusCode()).isEqualTo(200);
        return id;
    }

    private UUID createProject(String wish) throws Exception {
        return http.createProject(wish);
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        return http.call(method, path, body);
    }
}
