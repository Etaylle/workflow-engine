package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.TestHttp;
import engine.shared.SessionKey;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * The optional reviewer builder (D-103) through HTTP and the real PostgreSQL: Claude Code builds and Codex
 * reviews, both fake programs (src/test/resources/fake-cli). No test calls Anthropic or OpenAI.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "engine.execution.templates-dir=../templates",
        "engine.execution.create-database=false", "engine.execution.limit-wait-base-ms=20",
        "engine.execution.limit-wait-max-ms=80", "engine.execution.limit-max-retries=3"})
@Import(ScriptedAi.class)
class ReviewerRunIT {

    private static final Path BIN = fakePrograms();
    private static final Path WORK = tempDir("engine-reviewer-it-");

    @DynamicPropertySource
    static void fakeCommands(DynamicPropertyRegistry registry) {
        registry.add("engine.codex.cmd", () -> BIN.resolve("codex").toString());
        registry.add("engine.execution.claude-bin", () -> BIN.resolve("claude").toString());
        registry.add("engine.execution.workspace-dir", WORK::toString);
    }

    @Autowired private SessionKey sessionKey;
    @Autowired private ClaudeAuth auth;
    @Autowired private ToolProbe probe;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void reset() throws IOException {
        http = new TestHttp(port);
        sessionKey.clearAll();
        auth.choose("anthropic_key");
        probe.forget();
        Files.deleteIfExists(BIN.resolve("codex-logged-in"));
        Files.deleteIfExists(BIN.resolve("claude-args.log"));
        sessionKey.setAnthropic("sk-test-fake");
        Path state = BIN.resolve("fake-codex");
        for (String file : List.of("queue", "args.log", "env.log")) {
            Files.deleteIfExists(state.resolve(file));
        }
    }

    @Test
    void theReviewStepAndItsFixAttemptsRunWithTheReviewerAndEveryOtherStepWithTheBuilder() throws Exception {
        signIn();
        queue("ok", "badcheck", "ok");
        UUID project = confirmedProject();
        http.json("PATCH", "/api/projects/" + project, "{\"reviewerHarness\":\"codex_cli\","
                + "\"reviewerModel\":\"gpt-6-luna\",\"reviewerEffort\":\"low\"}");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("reviewerHarness").asString()).isEqualTo("codex_cli");
        assertThat(run.get("reviewerModel").asString()).isEqualTo("gpt-6-luna");
        assertThat(run.get("reviewerEffort").asString()).isEqualTo("low");
        int tasks = run.get("tasks").size();
        List<String> codex = Files.readAllLines(BIN.resolve("fake-codex/args.log"));
        List<String> claude = Files.readAllLines(BIN.resolve("claude-args.log"));
        assertThat(codex).as("plan review and check of every task and one fix attempt").hasSize(tasks * 2 + 1)
                .allSatisfy(call -> assertThat(call).contains("-m gpt-6-luna")
                        .contains("model_reasoning_effort=\"low\"")
                        .contains("reviewing work written by another AI builder"));
        assertThat(codex.get(0)).as("the second opinion comes before anything is built")
                .contains("SECOND OPINION ON THE PLAN").contains("docs/PLAN.md");
        assertThat(codex.get(2)).contains("Your previous attempt at this step failed");
        assertThat(claude).as("plan, build and ci of every task").hasSize(tasks * 3)
                .allSatisfy(call -> assertThat(call).doesNotContain("another AI builder")
                        .contains("--model fable"));
        JsonNode steps = run.get("tasks").get(0).get("steps");
        assertThat(steps).extracting(s -> s.get("key").asString())
                .containsExactly("plan", "plan-review", "build", "ci", "check");
        assertThat(steps.get(1).get("review").asBoolean()).isTrue();
        assertThat(steps.get(1).get("status").asString()).isEqualTo("done");
        assertThat(steps.get(1).get("builderHarness").asString()).isEqualTo("codex_cli");
        assertThat(steps.get(4).get("review").asBoolean()).isTrue();
        assertThat(steps.get(4).get("builderHarness").asString()).isEqualTo("codex_cli");
        assertThat(steps.get(4).get("builderModel").asString()).isEqualTo("gpt-6-luna");
        assertThat(steps.get(4).get("attempts").asInt()).isEqualTo(2);
        assertThat(steps.get(2).get("review").asBoolean()).isFalse();
        assertThat(steps.get(2).get("builderHarness").asString()).isEqualTo("claude_code");
        JsonNode report = http.json("GET", "/api/runs/" + run.get("id").asString() + "/report", null);
        assertThat(report.get("reviewerHarness").asString()).isEqualTo("codex_cli");
        assertThat(report.get("tasks").get(0).get("steps").get(4).get("builderModel").asString())
                .isEqualTo("gpt-6-luna");
    }

    @Test
    void withoutAReviewerEveryStepRunsWithTheBuilderAsBefore() throws Exception {
        signIn();
        UUID project = confirmedProject();

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("reviewerHarness").isNull()).isTrue();
        assertThat(BIN.resolve("fake-codex/args.log")).doesNotExist();
        assertThat(Files.readAllLines(BIN.resolve("claude-args.log"))).hasSize(run.get("tasks").size() * 4)
                .allSatisfy(call -> assertThat(call).doesNotContain("another AI builder"));
        JsonNode steps = run.get("tasks").get(0).get("steps");
        assertThat(steps).as("without a reviewer the second opinion on the plan is left out")
                .extracting(s -> s.get("key").asString()).containsExactly("plan", "build", "ci", "check");
        assertThat(steps.get(3).get("builderHarness").asString()).isEqualTo("claude_code");
    }

    @Test
    void aReviewerThatIsNotReadyIsA409BeforeTheRunStarts() throws Exception {
        UUID project = confirmedProject();
        http.json("PATCH", "/api/projects/" + project, "{\"reviewerHarness\":\"codex_cli\"}");

        HttpResponse<String> response = http.call("POST", "/api/projects/" + project + "/runs", null);

        assertThat(response.statusCode()).isEqualTo(409);
        JsonNode problem = TestHttp.JSON.readTree(response.body());
        assertThat(problem.get("title").asString()).isEqualTo("Reviewer not ready: Codex CLI");
        assertThat(problem.get("detail").asString()).contains("reviewer").contains("login");
        assertThat(http.call("GET", "/api/projects/" + project + "/runs/latest", null).statusCode())
                .as("no run was created").isEqualTo(404);
    }

    @Test
    void theReviewerMustDifferFromTheBuilderInBuilderOrModel() throws Exception {
        UUID project = http.answeredProject("A canteen app");
        String url = "/api/projects/" + project;

        HttpResponse<String> same = http.call("PATCH", url, "{\"reviewerHarness\":\"claude_code\"}");
        assertThat(same.statusCode()).isEqualTo(400);
        assertThat(same.body()).contains("Reviewer must differ from the builder");
        http.json("PATCH", url, "{\"builderModel\":\"sonnet\"}");
        assertThat(http.call("PATCH", url, "{\"reviewerHarness\":\"claude_code\",\"reviewerModel\":\"sonnet\"}")
                .statusCode()).isEqualTo(400);
        assertThat(http.call("PATCH", url, "{\"reviewerModel\":\"opus\"}").statusCode())
                .as("a model without a reviewer").isEqualTo(400);
        assertThat(http.call("PATCH", url, "{\"reviewerHarness\":\"codex_cli\",\"reviewerEffort\":\"max\"}")
                .statusCode()).as("the effort is checked for the reviewer's builder").isEqualTo(400);

        JsonNode other = http.json("PATCH", url, "{\"reviewerHarness\":\"claude_code\","
                + "\"reviewerModel\":\"opus\",\"reviewerEffort\":\"high\"}");
        assertThat(other.get("reviewerModel").asString()).isEqualTo("opus");
        assertThat(http.call("PATCH", url, "{\"builderModel\":\"opus\"}").statusCode())
                .as("changing the builder into the reviewer is refused too").isEqualTo(400);

        JsonNode codex = http.json("PATCH", url, "{\"reviewerHarness\":\"codex_cli\"}");
        assertThat(codex.get("reviewerHarness").asString()).isEqualTo("codex_cli");
        assertThat(codex.get("reviewerModel").isNull()).as("a new harness clears the model").isTrue();
        assertThat(codex.get("reviewerEffort").isNull()).isTrue();
        JsonNode none = http.json("PATCH", url, "{\"reviewerHarness\":\"\"}");
        assertThat(none.get("reviewerHarness").isNull()).isTrue();
    }

    @Test
    void aResumeKeepsTheReviewerWhateverTheProjectChoosesLater() throws Exception {
        signIn();
        queue("fail", "fail", "fail", "ok");
        UUID project = confirmedProject();
        http.json("PATCH", "/api/projects/" + project, "{\"reviewerHarness\":\"codex_cli\","
                + "\"reviewerModel\":\"gpt-6-luna\"}");
        String runId = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        JsonNode waiting = awaitEnd(runId);
        assertThat(waiting.get("status").asString()).isEqualTo("waiting");

        http.json("PATCH", "/api/projects/" + project, "{\"reviewerHarness\":\"\"}");
        http.call("POST", "/api/runs/" + runId + "/answer", "{\"questionId\":\""
                + waiting.get("question").get("id").asString() + "\",\"choice\":\"retry\"}");
        JsonNode run = awaitEnd(runId);

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("reviewerHarness").asString()).isEqualTo("codex_cli");
        assertThat(Files.readAllLines(BIN.resolve("fake-codex/args.log"))).hasSizeGreaterThanOrEqualTo(4)
                .allSatisfy(call -> assertThat(call).contains("-m gpt-6-luna"));
    }

    @Test
    void aRunWhoseReviewerIsOutOfTokensGoesOnWithoutTheSecondOpinionWhenTheClientSaysSo() throws Exception {
        signIn();
        queue("limit", "limit", "limit", "limit", "limit", "limit");
        UUID project = confirmedProject();
        http.json("PATCH", "/api/projects/" + project, "{\"reviewerHarness\":\"codex_cli\"}");
        String runId = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        JsonNode waiting = awaitStoppedRetrying(runId);
        assertThat(waiting.get("failureReason").asString()).contains("usage limit");
        int limited = Files.readAllLines(BIN.resolve("fake-codex/args.log")).size();

        JsonNode resumed = http.json("POST", "/api/runs/" + runId + "/resume", "{\"withoutReviewer\":true}");
        assertThat(resumed.get("reviewerHarness").isNull()).isTrue();
        JsonNode run = awaitEnd(runId);

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(Files.readAllLines(BIN.resolve("fake-codex/args.log")))
                .as("the reviewer is not called again").hasSize(limited);
        for (JsonNode task : run.get("tasks")) {
            JsonNode steps = task.get("steps");
            assertThat(steps).allSatisfy(s -> assertThat(s.get("status").asString()).isEqualTo("done"));
            assertThat(steps.get(4).get("builderHarness").asString()).as("the builder checks its own work")
                    .isEqualTo("claude_code");
        }
        assertThat(Files.readAllLines(BIN.resolve("claude-args.log")))
                .as("plan, build, ci and check of every task; no second opinion on the plan")
                .hasSize(run.get("tasks").size() * 4)
                .noneSatisfy(call -> assertThat(call).contains("SECOND OPINION ON THE PLAN"));
        assertThat(http.call("GET", "/api/runs/" + runId + "/events?after=0", null).body())
                .contains("goes on without the second opinion of Codex CLI").contains("left out");
    }

    private JsonNode awaitStoppedRetrying(String runId) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode run = http.json("GET", "/api/runs/" + runId, null);
            if (run.path("failureReason").asString("").contains("Still limited")) {
                return run;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The run did not stop retrying within 60 seconds");
    }

    @Test
    void aStepProfileKeepsTheReviewFlagInACopyAndTheShortProfileMarksCheck() throws Exception {
        JsonNode profiles = http.json("GET", "/api/settings/step-profiles", null);
        JsonNode full = profiles.valueStream().filter(p -> "16 steps".equals(p.get("name").asString()))
                .findFirst().orElseThrow();
        assertThat(full.get("steps").valueStream().filter(s -> s.get("review").asBoolean())
                .map(s -> s.get("key").asString()).toList())
                .containsExactly("09-verification", "11-review", "16-audit");
        JsonNode copy = http.json("POST", "/api/settings/step-profiles", "{\"fromId\":\""
                + full.get("id").asString() + "\",\"name\":\"Copy with review\"}");
        assertThat(copy.get("steps").valueStream().filter(s -> s.get("review").asBoolean()).count())
                .isEqualTo(3);
        http.call("DELETE", "/api/settings/step-profiles/" + copy.get("id").asString(), null);
        JsonNode shortProfile = profiles.valueStream().filter(p -> "Short".equals(p.get("name").asString()))
                .findFirst().orElseThrow();
        assertThat(shortProfile.get("steps").valueStream().filter(s -> s.get("review").asBoolean())
                .map(s -> s.get("key").asString()).toList()).containsExactly("plan-review", "check");
        assertThat(shortProfile.get("steps").valueStream().filter(s -> s.get("reviewerOnly").asBoolean())
                .map(s -> s.get("key").asString()).toList()).containsExactly("plan-review");
        JsonNode shortCopy = http.json("POST", "/api/settings/step-profiles", "{\"fromId\":\""
                + shortProfile.get("id").asString() + "\",\"name\":\"Copy with second opinion\"}");
        assertThat(shortCopy.get("steps").get(1).get("reviewerOnly").asBoolean()).isTrue();
        http.call("DELETE", "/api/settings/step-profiles/" + shortCopy.get("id").asString(), null);
    }

    private static void signIn() throws IOException {
        Files.writeString(BIN.resolve("codex-logged-in"), "yes");
    }

    private static void queue(String... modes) throws IOException {
        Path dir = Files.createDirectories(BIN.resolve("fake-codex"));
        Files.writeString(dir.resolve("queue"), String.join("\n", modes) + "\n");
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
            boolean busy = "preparing".equals(status) || "executing".equals(status)
                    || ("waiting".equals(status) && "tokens".equals(run.path("waitKind").asString("")));
            if (!busy) {
                return run;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The run did not end within 60 seconds");
    }

    private static Path tempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create a temporary folder", e);
        }
    }

    /** Copies the fake programs to a temporary folder of their own so tests can add state files there. */
    private static Path fakePrograms() {
        Path bin = tempDir("engine-fake-cli-");
        for (String name : List.of("codex", "claude")) {
            Path target = bin.resolve(name);
            try {
                Files.copy(Path.of("src/test/resources/fake-cli", name), target,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException("Could not copy the fake " + name, e);
            }
            if (!target.toFile().setExecutable(true)) {
                throw new IllegalStateException("Could not make " + target + " executable");
            }
        }
        return bin;
    }
}
