package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.TestHttp;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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

/** A run through HTTP and the real PostgreSQL; the builder is scripted, Claude is never called. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "engine.execution.workspace-dir=${java.io.tmpdir}/engine-it-workspaces",
        "engine.execution.template-dir=../templates/java-svelte-postgres",
        "engine.execution.create-database=false"})
@Import({ScriptedAi.class, RunIT.Config.class})
class RunIT {

    /** A builder that writes a file per step and fails or waits on demand. */
    static class ScriptedBuilder implements Builder {
        final Set<String> failSteps = new HashSet<>();
        final Set<String> failGates = new HashSet<>();
        volatile CountDownLatch hold;
        volatile boolean ready = true;

        void reset() {
            failSteps.clear();
            failGates.clear();
            hold = null;
            ready = true;
        }

        @Override
        public void checkReady() {
            if (!ready) {
                throw new engine.shared.ApiProblem(409, "No Anthropic key", "no key");
            }
        }

        @Override
        public StepOutcome run(StepRequest request) {
            await();
            if (failSteps.contains(request.stepKey())) {
                return new StepOutcome(false, new BigDecimal("0.25"), 2, 1, "error_max_turns",
                        "ran out of turns");
            }
            return new StubBuilder().run(request);
        }

        @Override
        public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir) {
            await();
            return failGates.isEmpty() ? new GateResult(true, List.of("CHECK PASSED"))
                    : new GateResult(false, List.of("tests failed: 1"));
        }

        private void await() {
            CountDownLatch latch = hold;
            if (latch != null) {
                try {
                    latch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        ScriptedBuilder scriptedBuilder() {
            return new ScriptedBuilder();
        }
    }

    @Autowired private ScriptedBuilder builder;
    @Autowired private RunService service;
    @Autowired private JdbcClient jdbc;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void reset() {
        builder.reset();
        http = new TestHttp(port);
    }

    @Test
    void tasksAreBuiltOneAfterTheOtherWithACheckpointPerStep() throws Exception {
        UUID project = confirmedProject();

        JsonNode started = http.json("POST", "/api/projects/" + project + "/runs", null);
        JsonNode run = awaitEnd(started.get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(http.statusOf(project)).isEqualTo("done");
        JsonNode tasks = run.get("tasks");
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).get("title").asString()).isEqualTo("Order a meal");
        assertThat(tasks.get(1).get("title").asString()).isEqualTo("Notify the student");
        for (JsonNode task : tasks) {
            assertThat(task.get("status").asString()).isEqualTo("done");
            assertThat(task.get("steps")).extracting(s -> s.get("key").asString())
                    .containsExactly("plan", "build", "ci", "check");
            task.get("steps").forEach(step -> {
                assertThat(step.get("status").asString()).isEqualTo("done");
                assertThat(step.get("commit").asString()).hasSize(40);
            });
        }
        assertThat(run.get("costUsd").asDouble()).isEqualTo(0.08);
        Path workspace = workspaceOf(run);
        assertThat(gitLog(workspace)).contains("Task 1 step plan green", "Task 2 step check green",
                "Task 1 done: Order a meal");
        assertThat(workspace.resolve("docs/task-1/STUB-plan.md")).exists();
        assertThat(Files.readString(workspace.resolve("CONTRACT.md")))
                .contains("Task \"Notify the student\"").contains("Already built in earlier tasks")
                .contains("Order a meal");
    }

    @Test
    void aFailedStepPausesTheRunWithTheReasonAndLeavesLaterWorkPending() throws Exception {
        UUID project = confirmedProject();
        builder.failSteps.add("build");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("paused");
        assertThat(run.get("failureReason").asString()).contains("Build with tests failed")
                .contains("ran out of turns");
        JsonNode first = run.get("tasks").get(0);
        assertThat(first.get("status").asString()).isEqualTo("failed");
        assertThat(first.get("steps").get(0).get("status").asString()).isEqualTo("done");
        assertThat(first.get("steps").get(1).get("status").asString()).isEqualTo("failed");
        assertThat(first.get("steps").get(2).get("status").asString()).isEqualTo("waiting");
        assertThat(run.get("tasks").get(1).get("status").asString()).isEqualTo("pending");
        assertThat(run.get("costUsd").asDouble()).isEqualTo(0.26);
        assertThat(http.statusOf(project)).isEqualTo("executing");
        assertThat(events(run.get("id").asString(), 0).get(
                events(run.get("id").asString(), 0).size() - 1).get("text").asString())
                .startsWith("paused: ");
    }

    @Test
    void aGateThatDoesNotPassPausesTheRunToo() throws Exception {
        UUID project = confirmedProject();
        builder.failGates.add("any");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("paused");
        assertThat(run.get("failureReason").asString()).contains("did not pass");
        assertThat(run.get("tasks").get(0).get("steps").get(1).get("status").asString())
                .isEqualTo("failed");
        assertThat(events(run.get("id").asString(), 0).toString()).contains("tests failed: 1");
    }

    @Test
    void eventsAreNumberedAndCanBeReplayedFromANumberAsJsonAndAsStream() throws Exception {
        UUID project = confirmedProject();
        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());
        String id = run.get("id").asString();
        long last = run.get("lastSeq").asLong();

        JsonNode all = events(id, 0);
        JsonNode tail = events(id, last - 2);

        assertThat(all).hasSize((int) last);
        for (int i = 0; i < all.size(); i++) {
            assertThat(all.get(i).get("seq").asLong()).isEqualTo(i + 1);
        }
        assertThat(tail).hasSize(2);
        assertThat(tail.get(0).get("seq").asLong()).isEqualTo(last - 1);
        assertThat(streamSeqs(id, "?after=" + (last - 2), 2)).containsExactly(last - 1, last);
        assertThat(streamSeqs(id, "", 3)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void theStreamDeliversLiveEventsAfterTheReplay() throws Exception {
        UUID project = confirmedProject();
        CountDownLatch hold = new CountDownLatch(1);
        builder.hold = hold;
        JsonNode started = http.json("POST", "/api/projects/" + project + "/runs", null);
        String id = started.get("id").asString();

        CompletableFuture<List<Long>> seen = CompletableFuture
                .supplyAsync(() -> streamSeqsUnchecked(id, "?after=0", 6));
        hold.countDown();

        assertThat(seen.get(30, TimeUnit.SECONDS)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
        awaitEnd(id);
    }

    @Test
    void zipHoldsTheWorkspaceWithoutGitAndWaitsForTheEndOfTheRun() throws Exception {
        UUID project = confirmedProject();
        CountDownLatch hold = new CountDownLatch(1);
        builder.hold = hold;
        String id = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();

        assertThat(http.call("GET", "/api/runs/" + id + "/zip", null).statusCode()).isEqualTo(409);
        hold.countDown();
        awaitEnd(id);

        HttpResponse<byte[]> zip = http.bytes("/api/runs/" + id + "/zip");
        assertThat(zip.statusCode()).isEqualTo(200);
        assertThat(zip.headers().firstValue("content-type")).contains("application/zip");
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip.body()))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        assertThat(names).contains("CONTRACT.md", "check.sh", "CLAUDE.md", "docs/task-1/STUB-plan.md",
                "docs/task-2/STUB-check.md", "backend/build.gradle.kts");
        assertThat(names).noneMatch(n -> n.startsWith(".git/") || n.contains("node_modules"));
    }

    @Test
    void aRunStartsOnlyOnAConfirmedContractAndOnlyOneAtATime() throws Exception {
        UUID unconfirmed = http.answeredProject("A canteen app");
        assertThat(http.call("POST", "/api/projects/" + unconfirmed + "/runs", null).statusCode())
                .isEqualTo(409);
        assertThat(http.call("POST", "/api/projects/" + UUID.randomUUID() + "/runs", null).statusCode())
                .isEqualTo(404);

        UUID project = confirmedProject();
        CountDownLatch hold = new CountDownLatch(1);
        builder.hold = hold;
        String id = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        HttpResponse<String> second = http.call("POST", "/api/projects/" + project + "/runs", null);
        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.body()).contains("already has a run under way");
        hold.countDown();
        awaitEnd(id);

        builder.ready = false;
        assertThat(http.call("POST", "/api/projects/" + project + "/runs", null).body())
                .contains("No Anthropic key");
        builder.ready = true;
        assertThat(http.call("POST", "/api/projects/" + project + "/runs", null).statusCode())
                .isEqualTo(201);
    }

    @Test
    void aRunUnderWayWhenTheEngineRestartsIsShownAsInterrupted() throws Exception {
        UUID project = confirmedProject();
        UUID run = UUID.randomUUID();
        jdbc.sql("insert into execution.runs (id, project_id, contract_version, status, cost_limit_usd,"
                        + " workspace_path, started_at) values (:id, :p, 1, 'executing', 10, '/nowhere', now())")
                .param("id", run).param("p", project).update();
        UUID task = UUID.randomUUID();
        jdbc.sql("insert into execution.task_runs (id, run_id, seq, title, status)"
                        + " values (:id, :r, 1, 'Order a meal', 'running')")
                .param("id", task).param("r", run).update();
        jdbc.sql("insert into execution.step_runs (id, task_run_id, seq, step_key, title, status)"
                        + " values (:id, :t, 1, 'plan', 'Plan', 'running')")
                .param("id", UUID.randomUUID()).param("t", task).update();

        service.recoverInterrupted();

        JsonNode view = http.json("GET", "/api/runs/" + run, null);
        assertThat(view.get("status").asString()).isEqualTo("interrupted");
        assertThat(view.get("failureReason").asString()).contains("engine restarted");
        assertThat(view.get("tasks").get(0).get("status").asString()).isEqualTo("failed");
        assertThat(view.get("tasks").get(0).get("steps").get(0).get("status").asString())
                .isEqualTo("failed");
        assertThat(events(run.toString(), 0).get(0).get("text").asString()).startsWith("interrupted");
        assertThat(http.json("GET", "/api/projects/" + project + "/runs/latest", null).get("id")
                .asString()).isEqualTo(run.toString());
    }

    @Test
    void deletingTheProjectRemovesItsRunsAndWorkspaces() throws Exception {
        UUID project = confirmedProject();
        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());
        Path workspace = workspaceOf(run);
        assertThat(workspace).exists();

        assertThat(http.call("DELETE", "/api/projects/" + project, null).statusCode()).isEqualTo(204);

        assertThat(workspace).doesNotExist();
        assertThat(http.call("GET", "/api/runs/" + run.get("id").asString(), null).statusCode())
                .isEqualTo(404);
    }

    private UUID confirmedProject() throws Exception {
        UUID id = http.answeredProject("A canteen app");
        assertThat(http.call("POST", "/api/projects/" + id + "/requirements", null).statusCode())
                .isEqualTo(200);
        assertThat(http.call("POST", "/api/projects/" + id + "/workflow", null).statusCode())
                .isEqualTo(200);
        assertThat(http.call("POST", "/api/projects/" + id + "/workflow/confirm", null).statusCode())
                .isEqualTo(201);
        return id;
    }

    private JsonNode awaitEnd(String runId) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode run = http.json("GET", "/api/runs/" + runId, null);
            String status = run.get("status").asString();
            if (!"preparing".equals(status) && !"executing".equals(status)) {
                return run;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The run did not end within 60 seconds");
    }

    private JsonNode events(String runId, long after) throws Exception {
        return http.json("GET", "/api/runs/" + runId + "/events?after=" + after, null);
    }

    private Path workspaceOf(JsonNode run) {
        String path = jdbc.sql("select workspace_path from execution.runs where id = :id")
                .param("id", UUID.fromString(run.get("id").asString())).query(String.class).single();
        return Path.of(path);
    }

    private static String gitLog(Path workspace) throws Exception {
        Process process = new ProcessBuilder("git", "log", "--format=%s").directory(workspace.toFile())
                .redirectErrorStream(true).start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor(30, TimeUnit.SECONDS);
        return out;
    }

    private List<Long> streamSeqsUnchecked(String runId, String query, int count) {
        try {
            return streamSeqs(runId, query, count);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Reads the first {@code count} event ids of the stream, then closes it. */
    private List<Long> streamSeqs(String runId, String query, int count) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/runs/" + runId + "/stream" + query))
                .header("Accept", "text/event-stream").build();
        HttpResponse<java.io.InputStream> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofInputStream());
        List<Long> seqs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while (seqs.size() < count && (line = reader.readLine()) != null) {
                if (line.startsWith("id:")) {
                    seqs.add(Long.parseLong(line.substring(3).trim()));
                }
            }
        }
        return seqs;
    }
}
