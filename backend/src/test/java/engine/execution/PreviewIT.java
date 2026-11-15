package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.TestHttp;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

/**
 * The preview of a run through HTTP and the real PostgreSQL: the stub builder finishes the tasks,
 * the app is a tiny fake script (src/test/resources/fake-template/start.sh) that prints a duck and
 * serves HTTP, so no Gradle and no npm run.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "engine.ai.stub=true", "engine.execution.workspace-dir=${java.io.tmpdir}/engine-preview-it",
        "engine.execution.template-dir=src/test/resources/fake-template",
        "engine.execution.create-database=false", "engine.execution.preview-seconds=6"})
@Import(ScriptedAi.class)
class PreviewIT {

    @Autowired private JdbcClient jdbc;
    @Autowired private RunPreviews previews;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void setUp() {
        http = new TestHttp(port);
    }

    @Test
    void aPreviewRunsTheCheckpointStreamsItsOutputAndStopsWithoutLeavingAnything() throws Exception {
        String run = finishedRun("A canteen app");

        JsonNode started = http.json("POST", "/api/runs/" + run + "/preview", null);
        JsonNode running = awaitStatus(run, "running");
        int appPort = running.get("port").asInt();

        assertThat(started.get("status").asString()).isEqualTo("starting");
        assertThat(running.get("checkpoint").asString()).hasSize(40);
        assertThat(running.get("expiresAt").asString()).isNotBlank();
        assertThat(get("http://127.0.0.1:" + appPort + "/")).isEqualTo(200);
        String output = appOutput(run);
        assertThat(output).contains("<(o )___").contains("listening on port " + appPort);
        assertThat(http.json("POST", "/api/runs/" + run + "/preview", null).get("port").asInt())
                .as("a second request returns the preview under way").isEqualTo(appPort);
        assertThat(previewDir(run)).exists();

        JsonNode stopped = http.json("DELETE", "/api/runs/" + run + "/preview", null);

        assertThat(stopped.get("status").asString()).isEqualTo("stopped");
        assertThat(get("http://127.0.0.1:" + appPort + "/")).isEqualTo(-1);
        assertThat(previewDir(run)).doesNotExist();
        assertThat(previewDir(run).resolveSibling(run + ".log")).doesNotExist();
    }

    @Test
    void aPreviewEndsByItselfWhenItsTimeIsUp() throws Exception {
        String run = finishedRun("A canteen app");
        http.json("POST", "/api/runs/" + run + "/preview", null);
        int appPort = awaitStatus(run, "running").get("port").asInt();

        JsonNode ended = awaitStatus(run, "stopped");

        assertThat(ended.get("message").asString()).contains("time of the preview is up");
        assertThat(get("http://127.0.0.1:" + appPort + "/")).isEqualTo(-1);
        assertThat(previewDir(run)).doesNotExist();
    }

    @Test
    void theEngineShuttingDownStopsEveryPreview() throws Exception {
        String run = finishedRun("A canteen app");
        http.json("POST", "/api/runs/" + run + "/preview", null);
        int appPort = awaitStatus(run, "running").get("port").asInt();

        previews.stopAll();

        assertThat(http.json("GET", "/api/runs/" + run + "/preview", null).get("status").asString())
                .isEqualTo("stopped");
        assertThat(get("http://127.0.0.1:" + appPort + "/")).isEqualTo(-1);
        assertThat(previewDir(run)).doesNotExist();
    }

    @Test
    void anOlderRunWithoutTheStartScriptGetsTheOneOfTheTemplate() throws Exception {
        String run = finishedRun("A canteen app");
        Workspace workspace = new Workspace(workspaceOf(run));
        workspace.git("rm", "-q", "start.sh");
        workspace.commit("An older run without start.sh");

        http.json("POST", "/api/runs/" + run + "/preview", null);

        assertThat(awaitStatus(run, "running").get("port").asInt()).isPositive();
        http.json("DELETE", "/api/runs/" + run + "/preview", null);
    }

    @Test
    void aStartScriptThatFailsMakesTheStatusFailedWithTheExitCodeAndItsOutput() throws Exception {
        String run = finishedRun("A canteen app");
        Workspace workspace = new Workspace(workspaceOf(run));
        Files.writeString(workspace.dir().resolve("start.sh"), "#!/usr/bin/env bash\necho boom\nexit 3\n");
        workspace.commit("A broken start script");

        http.json("POST", "/api/runs/" + run + "/preview", null);
        JsonNode failed = awaitStatus(run, "failed");

        assertThat(failed.get("message").asString()).contains("exit code 3");
        assertThat(appOutput(run)).contains("boom");
        assertThat(previewDir(run)).doesNotExist();
    }

    @Test
    void thereIsNothingToTryBeforeATaskIsDone() throws Exception {
        UUID project = http.answeredProject("A canteen app stub-slow:2000");
        confirm(project);
        String run = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();

        HttpResponse<String> response = http.call("POST", "/api/runs/" + run + "/preview", null);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(http.json("GET", "/api/runs/" + run + "/preview", null).get("status").asString())
                .isEqualTo("none");
        http.call("POST", "/api/runs/" + run + "/stop", null);
    }

    private String finishedRun(String wish) throws Exception {
        UUID project = http.answeredProject(wish);
        confirm(project);
        String run = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < deadline) {
            String status = http.json("GET", "/api/runs/" + run, null).get("status").asString();
            if ("done".equals(status)) {
                return run;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The run did not finish within 60 seconds");
    }

    private void confirm(UUID project) throws Exception {
        for (String step : new String[] {"/requirements", "/workflow", "/workflow/confirm"}) {
            assertThat(http.call("POST", "/api/projects/" + project + step, null).statusCode())
                    .isIn(200, 201);
        }
    }

    private JsonNode awaitStatus(String run, String wanted) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        JsonNode view = null;
        while (System.nanoTime() < deadline) {
            view = http.json("GET", "/api/runs/" + run + "/preview", null);
            if (wanted.equals(view.get("status").asString())) {
                return view;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The preview did not become " + wanted + ": " + view);
    }

    private String appOutput(String run) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        String text = "";
        while (System.nanoTime() < deadline) {
            StringBuilder lines = new StringBuilder();
            http.json("GET", "/api/runs/" + run + "/events?before=" + Long.MAX_VALUE + "&kind=app&limit=100",
                    null).forEach(e -> lines.append(e.get("text").asString()).append('\n'));
            text = lines.toString();
            if (!text.isBlank()) {
                return text;
            }
            Thread.sleep(100);
        }
        return text;
    }

    private Path previewDir(String run) {
        return workspaceOf(run).getParent().resolve("previews").resolve(run);
    }

    private Path workspaceOf(String run) {
        return Path.of(jdbc.sql("select workspace_path from execution.runs where id = :id")
                .param("id", UUID.fromString(run)).query(String.class).single());
    }

    /** The status code of a GET, or -1 when nothing answers. */
    private static int get(String url) throws InterruptedException {
        try {
            return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3)).build(), HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        } catch (java.io.IOException e) {
            return -1;
        }
    }
}
