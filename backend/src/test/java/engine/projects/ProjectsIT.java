package engine.projects;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.TestHttp;
import engine.shared.SessionKey;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Rename and delete of projects through HTTP and the real PostgreSQL. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedAi.class)
class ProjectsIT {

    @Autowired private SessionKey sessionKey;
    @Autowired private JdbcClient jdbc;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void reset() {
        sessionKey.set("sk-test");
        http = new TestHttp(port);
    }

    @Test
    void renameChangesTheNameAndRejectsBlankOrUnknown() throws Exception {
        UUID id = http.createProject("A canteen app");

        assertThat(http.json("PATCH", "/api/projects/" + id, "{\"name\":\" Canteen \"}").get("name").asString())
                .isEqualTo("Canteen");
        assertThat(http.call("PATCH", "/api/projects/" + id, "{\"name\":\"  \"}").statusCode()).isEqualTo(400);
        assertThat(http.call("PATCH", "/api/projects/" + id, "{\"name\":\"" + "x".repeat(121) + "\"}")
                .statusCode()).isEqualTo(400);
        assertThat(http.call("PATCH", "/api/projects/" + UUID.randomUUID(), "{\"name\":\"a\"}").statusCode())
                .isEqualTo(404);
    }

    @Test
    void deleteRemovesAConfirmedProjectWithEverythingItHolds() throws Exception {
        UUID id = http.answeredProject("A canteen app");
        http.call("POST", "/api/projects/" + id + "/requirements", null);
        http.call("POST", "/api/projects/" + id + "/workflow", null);
        assertThat(http.call("POST", "/api/projects/" + id + "/workflow/confirm", null).statusCode())
                .isEqualTo(201);
        UUID other = http.createProject("Another app");

        assertThat(http.call("DELETE", "/api/projects/" + id, null).statusCode()).isEqualTo(204);

        assertThat(http.call("GET", "/api/projects/" + id, null).statusCode()).isEqualTo(404);
        assertThat(http.call("GET", "/api/projects/" + other, null).statusCode()).isEqualTo(200);
        for (String table : new String[] {"questions.questions", "documents.requirements",
                "workflows.workflows", "workflows.contracts", "projects.project_briefs"}) {
            assertThat(jdbc.sql("select count(*) from " + table + " where project_id = :p")
                    .param("p", id).query(Long.class).single()).as(table).isZero();
        }
        assertThat(http.call("DELETE", "/api/projects/" + id, null).statusCode()).isEqualTo(404);
    }

    @Test
    void createTakesAGivenNameOrFallsBackToTheWish() throws Exception {
        String named = http.json("POST", "/api/projects",
                "{\"wish\":\"A canteen app\",\"name\":\" Canteen pre-order \"}").get("name").asString();
        String unnamed = http.json("POST", "/api/projects", "{\"wish\":\"A canteen app\"}")
                .get("name").asString();

        assertThat(named).isEqualTo("Canteen pre-order");
        assertThat(unnamed).isEqualTo("A canteen app");
        assertThat(http.call("POST", "/api/projects",
                "{\"wish\":\"x\",\"name\":\"" + "n".repeat(121) + "\"}").statusCode()).isEqualTo(400);
    }

    @Test
    void changingTheWishMovesTheProjectBackToDraftButRenamingDoesNot() throws Exception {
        UUID id = http.answeredProject("A canteen app");

        var renamed = http.json("PATCH", "/api/projects/" + id, "{\"name\":\"Same wish\"}");
        assertThat(renamed.get("status").asString()).isEqualTo("answered");

        var sameWish = http.json("PATCH", "/api/projects/" + id, "{\"wish\":\"A canteen app\"}");
        assertThat(sameWish.get("status").asString()).isEqualTo("answered");

        var changed = http.json("PATCH", "/api/projects/" + id, "{\"wish\":\" A bike shop app \"}");
        assertThat(changed.get("status").asString()).isEqualTo("draft");
        assertThat(changed.get("wish").asString()).isEqualTo("A bike shop app");
        assertThat(changed.get("name").asString()).isEqualTo("Same wish");
    }

    @Test
    void updateRejectsEmptyRequestAndBlankWish() throws Exception {
        UUID id = http.createProject("A canteen app");

        assertThat(http.call("PATCH", "/api/projects/" + id, "{}").statusCode()).isEqualTo(400);
        assertThat(http.call("PATCH", "/api/projects/" + id, "{\"wish\":\"  \"}").statusCode())
                .isEqualTo(400);
    }
}
