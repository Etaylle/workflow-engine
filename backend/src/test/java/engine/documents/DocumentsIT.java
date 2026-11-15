package engine.documents;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.TestHttp;
import engine.shared.SessionKey;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;

/** The two project documents through HTTP and the real PostgreSQL. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedAi.class)
class DocumentsIT {

    @Autowired private SessionKey sessionKey;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void reset() {
        sessionKey.setAnthropic("sk-test");
        http = new TestHttp(port);
    }

    @Test
    void requirementsAndPlanAreViewedAndDownloaded() throws Exception {
        UUID id = http.answeredProject("A canteen app");
        http.call("POST", "/api/projects/" + id + "/requirements", null);
        http.call("POST", "/api/projects/" + id + "/workflow", null);

        JsonNode requirements = http.json("GET", "/api/projects/" + id + "/documents/requirements", null);
        JsonNode plan = http.json("GET", "/api/projects/" + id + "/documents/plan", null);

        assertThat(requirements.get("title").asString()).startsWith("Requirements: ");
        assertThat(requirements.toString()).contains("FR-001", "AI proposal");
        assertThat(plan.get("title").asString()).startsWith("Project plan: ");
        assertThat(plan.toString()).contains("Order a meal", "software tasks");

        var docx = http.bytes("/api/projects/" + id + "/documents/plan/download?format=docx");
        assertThat(docx.statusCode()).isEqualTo(200);
        assertThat(docx.headers().firstValue("content-disposition").orElse("")).contains("plan.docx");
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx.body()))) {
            assertThat(zip.getNextEntry()).isNotNull();
        }
        var md = http.bytes("/api/projects/" + id + "/documents/requirements/download?format=md");
        assertThat(new String(md.body(), java.nio.charset.StandardCharsets.UTF_8)).contains("## Epic");
    }

    @Test
    void tooEarlyUnknownKindAndUnknownFormatAreRefused() throws Exception {
        UUID id = http.answeredProject("A canteen app");

        assertThat(http.call("GET", "/api/projects/" + id + "/documents/requirements", null).statusCode())
                .isEqualTo(409);
        assertThat(http.call("GET", "/api/projects/" + id + "/documents/plan", null).statusCode())
                .isEqualTo(409);
        assertThat(http.call("GET", "/api/projects/" + id + "/documents/contract", null).statusCode())
                .isEqualTo(404);
        http.call("POST", "/api/projects/" + id + "/requirements", null);
        assertThat(http.bytes("/api/projects/" + id + "/documents/requirements/download?format=pdf")
                .statusCode()).isEqualTo(400);
        assertThat(http.call("GET", "/api/projects/" + UUID.randomUUID() + "/documents/plan", null)
                .statusCode()).isEqualTo(404);
    }
}
