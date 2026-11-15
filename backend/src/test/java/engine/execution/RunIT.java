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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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
        "engine.execution.create-database=false", "engine.execution.limit-wait-base-ms=20",
        "engine.execution.limit-wait-max-ms=80", "engine.execution.limit-max-retries=3"})
@Import({ScriptedAi.class, RunIT.Config.class})
class RunIT {

    /** A builder that writes a file per step and fails or waits on demand. */
    static class ScriptedBuilder implements Builder {
        final Set<String> failSteps = new HashSet<>();
        final Set<String> failGates = new HashSet<>();
        /** Step key to the number of invocations that still fail before it succeeds. */
        final Map<String, Integer> failStepTimes = new HashMap<>();
        /** Step key to the number of invocations that still answer "rate limit" before it works. */
        final Map<String, Integer> limitTimes = new HashMap<>();
        volatile int failGateTimes;
        final List<StepRequest> requests = new CopyOnWriteArrayList<>();
        volatile CountDownLatch hold;
        volatile CountDownLatch entered = new CountDownLatch(1);
        volatile boolean ready = true;

        void reset() {
            failSteps.clear();
            failGates.clear();
            failStepTimes.clear();
            limitTimes.clear();
            failGateTimes = 0;
            requests.clear();
            hold = null;
            entered = new CountDownLatch(1);
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
            entered.countDown();
            await();
            requests.add(request);
            if (limitTimes.merge(request.stepKey(), -1, Integer::sum) >= 0) {
                return new StepOutcome(false, BigDecimal.ZERO, 0, 1, "rate_limit",
                        "API Error: 429 rate limit exceeded");
            }
            if (failSteps.contains(request.stepKey())
                    || failStepTimes.merge(request.stepKey(), -1, Integer::sum) >= 0) {
                return new StepOutcome(false, new BigDecimal("0.25"), 2, 1, "error_max_turns",
                        "ran out of turns");
            }
            return new StubBuilder().run(request);
        }

        @Override
        public GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir) {
            await();
            if (failGateTimes > 0) {
                failGateTimes--;
                return new GateResult(false, List.of("tests failed: 1"));
            }
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
    void aStepThatStaysBrokenMakesTheRunWaitWithOneQuestionAndLeavesLaterWorkPending() throws Exception {
        UUID project = confirmedProject();
        builder.failSteps.add("build");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("waiting");
        assertThat(run.get("waitKind").asString()).isEqualTo("question");
        assertThat(run.get("question").get("text").asString()).contains("Build with tests")
                .contains("could not be fixed after 3 attempts").contains("ran out of turns");
        assertThat(run.get("question").get("options")).extracting(JsonNode::asString)
                .containsExactly("retry", "skip", "stop");
        assertThat(run.get("failureReason").asString()).contains("ran out of turns");
        JsonNode first = run.get("tasks").get(0);
        assertThat(first.get("status").asString()).isEqualTo("failed");
        assertThat(first.get("steps").get(0).get("status").asString()).isEqualTo("done");
        assertThat(first.get("steps").get(1).get("status").asString()).isEqualTo("failed");
        assertThat(first.get("steps").get(2).get("status").asString()).isEqualTo("waiting");
        assertThat(run.get("tasks").get(1).get("status").asString()).isEqualTo("pending");
        assertThat(run.get("costUsd").asDouble()).isEqualTo(0.76);
        assertThat(first.get("steps").get(1).get("attempts").asInt()).isEqualTo(3);
        assertThat(http.statusOf(project)).isEqualTo("executing");
        assertThat(events(run.get("id").asString(), 0).get(
                events(run.get("id").asString(), 0).size() - 1).get("text").asString())
                .startsWith("waiting: ");
    }

    @Test
    void aFailedCheckSendsTheStepBackWithTheFailureAndTheSecondAttemptPasses() throws Exception {
        UUID project = confirmedProject();
        builder.failGateTimes = 1;

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        JsonNode build = run.get("tasks").get(0).get("steps").get(1);
        assertThat(build.get("attempts").asInt()).isEqualTo(2);
        List<StepRequest> buildCalls = builder.requests.stream()
                .filter(r -> r.stepKey().equals("build")).toList();
        assertThat(buildCalls.get(0).prompt()).doesNotContain("previous attempt");
        assertThat(buildCalls.get(1).prompt()).contains("previous attempt").contains("tests failed: 1");
        List<String> calls = jdbc.sql("select outcome || ':' || coalesce(check_output, '')"
                        + " from execution.agent_calls where step_run_id = :s order by attempt")
                .param("s", UUID.fromString(build.get("id").asString())).query(String.class).list();
        assertThat(calls).hasSize(2);
        assertThat(calls.get(0)).startsWith("check_failed:").contains("tests failed: 1");
        assertThat(calls.get(1)).startsWith("success:");
        assertThat(events(run.get("id").asString(), 0).toString()).contains("Fix attempt 1 of 2");
    }

    @Test
    void aFailingBuilderIsRetriedAndTheStepSucceedsOnTheLastAttempt() throws Exception {
        UUID project = confirmedProject();
        builder.failStepTimes.put("build", 2);

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("tasks").get(0).get("steps").get(1).get("attempts").asInt()).isEqualTo(3);
    }

    @Test
    void theClientAnswersTryAgainWithAHintAndTheRunContinuesFromTheStep() throws Exception {
        UUID project = confirmedProject();
        builder.failSteps.add("build");
        JsonNode waiting = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());
        String id = waiting.get("id").asString();
        String questionId = waiting.get("question").get("id").asString();
        builder.failSteps.clear();
        builder.requests.clear();

        assertThat(answer(id, UUID.randomUUID().toString(), "retry", null).statusCode()).isEqualTo(409);
        assertThat(answer(id, questionId, "later", null).statusCode()).isEqualTo(400);
        assertThat(answer(id, questionId, "retry", "Keep it simple").statusCode()).isEqualTo(200);
        JsonNode run = awaitEnd(id);

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("question").isNull()).isTrue();
        assertThat(builder.requests.get(0).stepKey()).isEqualTo("build");
        assertThat(builder.requests.get(0).prompt()).contains("Keep it simple");
        assertThat(builder.requests.stream().map(StepRequest::stepKey).filter("plan"::equals)
                .count()).isEqualTo(1);
        assertThat(answer(id, questionId, "retry", null).statusCode()).isEqualTo(409);
        assertThat(http.statusOf(project)).isEqualTo("done");
    }

    @Test
    void theClientSkipsTheStepAndTheRunGoesOn() throws Exception {
        UUID project = confirmedProject();
        builder.failSteps.add("build");
        JsonNode waiting = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());
        String id = waiting.get("id").asString();
        builder.failSteps.clear();

        assertThat(answer(id, waiting.get("question").get("id").asString(), "skip", null).statusCode())
                .isEqualTo(200);
        JsonNode run = awaitEnd(id);

        assertThat(run.get("status").asString()).isEqualTo("done");
        JsonNode build = run.get("tasks").get(0).get("steps").get(1);
        assertThat(build.get("status").asString()).isEqualTo("done");
        assertThat(build.get("result").asString()).isEqualTo("Skipped by the client");
        assertThat(builder.requests.stream().filter(r -> r.stepKey().equals("build")).count())
                .isEqualTo(3 + 1);
    }

    @Test
    void aGateThatNeverPassesMakesTheRunWaitToo() throws Exception {
        UUID project = confirmedProject();
        builder.failGates.add("any");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("waiting");
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

    @Test
    void stoppingABuildEndsItFailedAndLeavesTheWorkspaceAtTheLastCheckpoint() throws Exception {
        UUID project = confirmedProject();
        CountDownLatch hold = new CountDownLatch(1);
        builder.hold = hold;
        String id = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        assertThat(builder.entered.await(30, TimeUnit.SECONDS)).isTrue();

        JsonNode stopped = http.json("POST", "/api/runs/" + id + "/stop", null);
        assertThat(stopped.get("status").asString()).isEqualTo("failed");
        assertThat(stopped.get("failureReason").asString()).isEqualTo("Stopped by the client");
        assertThat(http.call("GET", "/api/runs/" + id + "/zip", null).statusCode()).isEqualTo(409);
        hold.countDown();
        awaitIdle(id);

        JsonNode run = http.json("GET", "/api/runs/" + id, null);
        assertThat(run.get("status").asString()).isEqualTo("failed");
        assertThat(run.get("tasks").get(0).get("steps").get(0).get("status").asString())
                .isEqualTo("failed");
        assertThat(run.get("tasks").get(0).get("steps").get(1).get("status").asString())
                .isEqualTo("waiting");
        Path workspace = workspaceOf(run);
        assertThat(workspace.resolve("docs/STUB-plan.md")).doesNotExist();
        assertThat(gitStatus(workspace)).isEmpty();
        assertThat(builder.requests).hasSize(1);
        assertThat(events(id, 0).toString()).contains("failed: stopped by the client");
        assertThat(http.call("GET", "/api/runs/" + id + "/zip", null).statusCode()).isEqualTo(200);
        assertThat(http.call("POST", "/api/runs/" + id + "/stop", null).statusCode()).isEqualTo(409);
        assertThat(http.call("POST", "/api/runs/" + id + "/resume", null).statusCode()).isEqualTo(409);
    }

    @Test
    void aWaitingRunCanBeStoppedByTheClientOrByAnsweringStop() throws Exception {
        UUID project = confirmedProject();
        builder.failSteps.add("build");
        JsonNode waiting = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());
        String id = waiting.get("id").asString();

        JsonNode run = http.json("POST", "/api/runs/" + id + "/stop", null);

        assertThat(run.get("status").asString()).isEqualTo("failed");
        assertThat(run.get("question").isNull()).isTrue();

        JsonNode second = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());
        assertThat(answer(second.get("id").asString(), second.get("question").get("id").asString(),
                "stop", null).statusCode()).isEqualTo(200);
        assertThat(http.json("GET", "/api/runs/" + second.get("id").asString(), null).get("status")
                .asString()).isEqualTo("failed");
    }

    @Test
    void anInterruptedRunNeedsTheKeyAgainThenContinuesFromTheLastCheckpoint() throws Exception {
        UUID project = confirmedProject();
        builder.failSteps.add("build");
        JsonNode waiting = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());
        String id = waiting.get("id").asString();
        assertThat(http.call("POST", "/api/runs/" + id + "/resume", null).statusCode()).isEqualTo(409);
        simulateRestart(id);
        Path workspace = workspaceOf(waiting);
        Files.writeString(workspace.resolve("docs/half-written.md"), "not checkpointed");
        Files.writeString(workspace.resolve("CONTRACT.md"), "broken");
        builder.failSteps.clear();
        builder.requests.clear();

        builder.ready = false;
        HttpResponse<String> noKey = http.call("POST", "/api/runs/" + id + "/resume", null);
        assertThat(noKey.statusCode()).isEqualTo(409);
        assertThat(noKey.body()).contains("No Anthropic key");
        assertThat(http.json("GET", "/api/runs/" + id, null).get("status").asString())
                .isEqualTo("interrupted");
        builder.ready = true;
        JsonNode resumed = http.json("POST", "/api/runs/" + id + "/resume", null);
        assertThat(resumed.get("resumeCount").asInt()).isEqualTo(1);
        JsonNode run = awaitEnd(id);

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(workspace.resolve("docs/half-written.md")).doesNotExist();
        assertThat(builder.requests.get(0).stepKey()).isEqualTo("build");
        assertThat(builder.requests.stream().map(StepRequest::stepKey).filter("plan"::equals).count())
                .as("only the plan of task 2 runs, not the finished plan of task 1").isEqualTo(1);
        assertThat(events(id, 0).toString()).contains("resumed (1 of 3)")
                .contains("continued from the last checkpoint");
        assertThat(Files.readString(workspace.resolve("CONTRACT.md"))).isNotEqualTo("broken");
    }

    @Test
    void aRunIsResumedAtMostThreeTimes() throws Exception {
        UUID project = confirmedProject();
        builder.failSteps.add("build");
        String id = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString()).get("id").asString();
        simulateRestart(id);
        jdbc.sql("update execution.runs set resume_count = 3 where id = :id")
                .param("id", UUID.fromString(id)).update();

        HttpResponse<String> response = http.call("POST", "/api/runs/" + id + "/resume", null);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("3 times");
        JsonNode run = http.json("GET", "/api/runs/" + id, null);
        assertThat(run.get("status").asString()).isEqualTo("failed");
        assertThat(run.get("failureReason").asString()).contains("resumed 3 times");
    }

    @Test
    void aRateLimitMakesTheRunWaitAndTryAgainWithGrowingDelaysInsteadOfFailing() throws Exception {
        UUID project = confirmedProject();
        builder.limitTimes.put("build", 2);

        String id = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        JsonNode run = awaitStatus(id, "done");

        assertThat(run.get("tasks").get(0).get("steps").get(1).get("attempts").asInt()).isEqualTo(3);
        List<String> outcomes = jdbc.sql("select c.outcome from execution.agent_calls c join"
                        + " execution.step_runs s on s.id = c.step_run_id where c.step_run_id = :s"
                        + " order by c.attempt")
                .param("s", UUID.fromString(run.get("tasks").get(0).get("steps").get(1).get("id")
                        .asString())).query(String.class).list();
        assertThat(outcomes).containsExactly("rate_limit", "rate_limit", "success");
        String log = events(id, 0).toString();
        assertThat(log).contains("waiting: Waiting for tokens: Build with tests failed: API Error: 429")
                .contains("Try 1 of 3").contains("Try 2 of 3").contains("executing: trying again");
        assertThat(log).doesNotContain("Fix attempt").doesNotContain("question for the client");
    }

    @Test
    void whenTheLimitDoesNotLiftTheRunStopsRetryingAndWaitsUntilTheClientContinuesIt() throws Exception {
        UUID project = confirmedProject();
        builder.limitTimes.put("build", 100);

        String id = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        JsonNode waiting = awaitStatus(id, "waiting", "Still limited");

        assertThat(waiting.get("waitKind").asString()).isEqualTo("tokens");
        assertThat(waiting.get("failureReason").asString()).contains("Still limited after 3 tries");
        assertThat(builder.requests.stream().filter(r -> r.stepKey().equals("build")).count())
                .isEqualTo(4);
        assertThat(waiting.get("tasks").get(0).get("steps").get(1).get("status").asString())
                .isEqualTo("waiting");
        builder.limitTimes.clear();
        assertThat(http.call("POST", "/api/runs/" + id + "/answer", "{}").statusCode()).isNotEqualTo(200);

        assertThat(http.json("POST", "/api/runs/" + id + "/resume", null).get("status").asString())
                .isEqualTo("executing");
        assertThat(awaitStatus(id, "done").get("status").asString()).isEqualTo("done");
    }

    @Test
    void theCostLimitMakesTheRunWaitUntilTheClientRaisesIt() throws Exception {
        UUID project = confirmedProject();
        CountDownLatch hold = new CountDownLatch(1);
        builder.hold = hold;
        String id = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        assertThat(builder.entered.await(30, TimeUnit.SECONDS)).isTrue();
        jdbc.sql("update execution.runs set cost_limit_usd = 0.2 where id = :id")
                .param("id", UUID.fromString(id)).update();
        hold.countDown();

        JsonNode waiting = awaitStatus(id, "waiting", "cost limit");

        assertThat(waiting.get("waitKind").asString()).isEqualTo("cost_limit");
        assertThat(waiting.get("tasks").get(0).get("steps").get(0).get("status").asString())
                .isEqualTo("done");
        assertThat(waiting.get("tasks").get(0).get("steps").get(1).get("status").asString())
                .isEqualTo("waiting");
        assertThat(http.call("POST", "/api/runs/" + id + "/resume", null).statusCode()).isEqualTo(400);
        assertThat(http.call("POST", "/api/runs/" + id + "/resume", "{\"costLimitUsd\":0.25}")
                .statusCode()).isEqualTo(400);
        assertThat(http.json("GET", "/api/runs/" + id, null).get("status").asString()).isEqualTo("waiting");

        JsonNode resumed = http.json("POST", "/api/runs/" + id + "/resume", "{\"costLimitUsd\":5}");
        assertThat(resumed.get("costLimitUsd").asDouble()).isEqualTo(5.0);
        JsonNode run = awaitStatus(id, "done");
        assertThat(run.get("tasks")).allSatisfy(t -> assertThat(t.get("status").asString())
                .isEqualTo("done"));
        assertThat(builder.requests.stream().map(StepRequest::stepKey).filter("plan"::equals).count())
                .isEqualTo(2);
    }

    @Test
    void aRunWaitingForTokensIsInterruptedByAnEngineRestartAndCanBeResumed() throws Exception {
        UUID project = confirmedProject();
        builder.limitTimes.put("build", 100);
        String id = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        awaitStatus(id, "waiting", "Still limited");

        service.recoverInterrupted();
        assertThat(http.json("GET", "/api/runs/" + id, null).get("status").asString())
                .isEqualTo("interrupted");
        builder.limitTimes.clear();
        assertThat(http.json("POST", "/api/runs/" + id + "/resume", null).get("resumeCount").asInt())
                .isEqualTo(1);
        assertThat(awaitStatus(id, "done").get("status").asString()).isEqualTo("done");
    }

    /** Polls until the run has this status, its thread has ended and the reason contains the text. */
    private JsonNode awaitStatus(String runId, String status, String... reason) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode run = http.json("GET", "/api/runs/" + runId, null);
            boolean reasonOk = reason.length == 0 || (run.hasNonNull("failureReason")
                    && run.get("failureReason").asString().contains(reason[0]));
            if (status.equals(run.get("status").asString()) && reasonOk
                    && http.call("GET", "/api/runs/" + runId + "/zip", null).statusCode() == 200) {
                return run;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("The run did not reach " + status + " within 60 seconds");
    }

    /** The engine restarts while the run is under way: the row says executing, no thread exists. */
    private void simulateRestart(String runId) throws Exception {
        jdbc.sql("update execution.runs set status = 'executing', wait_kind = null where id = :id")
                .param("id", UUID.fromString(runId)).update();
        service.recoverInterrupted();
        assertThat(http.json("GET", "/api/runs/" + runId, null).get("status").asString())
                .isEqualTo("interrupted");
    }

    private void awaitIdle(String runId) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            if (http.call("GET", "/api/runs/" + runId + "/zip", null).statusCode() == 200) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The run thread did not end within 30 seconds");
    }

    private static String gitStatus(Path workspace) throws Exception {
        Process process = new ProcessBuilder("git", "status", "--porcelain").directory(workspace.toFile())
                .redirectErrorStream(true).start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor(30, TimeUnit.SECONDS);
        return out.strip();
    }

    private HttpResponse<String> answer(String runId, String questionId, String choice, String hint)
            throws Exception {
        String body = "{\"questionId\":\"" + questionId + "\",\"choice\":\"" + choice + "\""
                + (hint == null ? "" : ",\"hint\":\"" + hint + "\"") + "}";
        return http.call("POST", "/api/runs/" + runId + "/answer", body);
    }

    @Test
    void theRunUsesTheStepsAndTheLimitTheProjectChoseAndNamesTheTask() throws Exception {
        UUID project = confirmedProject();
        String fullId = null;
        for (JsonNode profile : http.json("GET", "/api/settings/step-profiles", null)) {
            if (profile.get("name").asString().equals("16 steps")) {
                fullId = profile.get("id").asString();
            }
        }
        http.json("PUT", "/api/projects/" + project + "/settings",
                "{\"stepProfileId\":\"" + fullId + "\",\"costLimitUsd\":7.5}");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("costLimitUsd").asDouble()).isEqualTo(7.5);
        assertThat(run.get("tasks").get(0).get("steps")).hasSize(16);
        assertThat(builder.requests.get(0).prompt()).contains("\"Order a meal\"").doesNotContain("{task}");
        http.json("PUT", "/api/projects/" + project + "/settings", "{\"stepProfileId\":null}");
        assertThat(http.json("GET", "/api/runs/" + run.get("id").asString(), null).get("tasks").get(0)
                .get("steps")).as("a run keeps the steps it started with").hasSize(16);
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
