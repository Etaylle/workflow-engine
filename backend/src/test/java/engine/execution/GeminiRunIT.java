package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.TestHttp;
import engine.shared.AiProvider;
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
 * Runs through HTTP and the real PostgreSQL with the real Gemini CLI builder and Claude Code builder
 * classes, but a fake {@code gemini} and {@code claude} program (src/test/resources/fake-cli). No
 * test calls Google or Anthropic.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "engine.execution.templates-dir=../templates",
        "engine.execution.create-database=false", "engine.execution.limit-wait-base-ms=20",
        "engine.execution.limit-wait-max-ms=80", "engine.execution.limit-max-retries=3"})
@Import(ScriptedAi.class)
class GeminiRunIT {

    private static final String GEMINI_KEY = "AIzaSyFAKE-test-key-0123456789abcdef";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/openai/";
    private static final Path BIN = fakePrograms();
    private static final Path WORK = tempDir("engine-gemini-it-");

    @DynamicPropertySource
    static void fakeCommands(DynamicPropertyRegistry registry) {
        registry.add("engine.gemini.cmd", () -> BIN.resolve("gemini").toString());
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
        Files.deleteIfExists(BIN.resolve("logged-in"));
        Path state = WORK.resolve("fake-gemini");
        for (String file : List.of("queue", "args.log", "env.log")) {
            Files.deleteIfExists(state.resolve(file));
        }
    }

    @Test
    void aGeminiRunBuildsWithTheKeyOnlyInTheEnvironmentOfTheChildAndRecordsTokens() throws Exception {
        setGeminiKey();
        UUID project = confirmedProject("gemini_cli");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("harness").asString()).isEqualTo("gemini_cli");
        assertThat(run.get("costUsd").asDouble()).as("free tier: cost 0").isZero();
        JsonNode step = run.get("tasks").get(0).get("steps").get(0);
        assertThat(step.get("tokens").asLong()).isEqualTo(1234);
        assertThat(step.get("turns").asInt()).isEqualTo(3);
        String args = Files.readString(WORK.resolve("fake-gemini/args.log"));
        assertThat(args).contains("-m gemini-3.1-flash-lite").contains("-o stream-json")
                .contains("--approval-mode auto_edit").contains("--policy").contains("GEMINI.md")
                .doesNotContain(GEMINI_KEY);
        assertThat(Files.readString(WORK.resolve("fake-gemini/env.log"))).contains("key=" + GEMINI_KEY)
                .contains(WORK.resolve(".gemini-home").toString());
        assertThat(everythingTheEngineShows(run)).doesNotContain(GEMINI_KEY);
        assertThat(WORK.resolve(".gemini-home/policy.toml")).content().contains("run_shell_command")
                .contains("\"./check.sh\"").doesNotContain("\"rm\"");
    }

    @Test
    void aMissingGeminiKeyIsAClear409NamingTheBuilderAndWhatIsMissing() throws Exception {
        UUID project = confirmedProject("gemini_cli");

        HttpResponse<String> response = http.call("POST", "/api/projects/" + project + "/runs", null);

        assertThat(response.statusCode()).isEqualTo(409);
        JsonNode problem = TestHttp.JSON.readTree(response.body());
        assertThat(problem.get("title").asString()).isEqualTo("Builder not ready: Gemini CLI");
        assertThat(problem.get("detail").asString()).contains("Gemini API key")
                .doesNotContain("Anthropic");
    }

    @Test
    void aClaudeCodeProjectNeedsTheAnthropicKeyOrTheSignInOfThisMachine() throws Exception {
        setGeminiKey();
        UUID project = confirmedProject("claude_code");
        sessionKey.clear(AiProvider.ANTHROPIC);

        HttpResponse<String> noKey = http.call("POST", "/api/projects/" + project + "/runs", null);

        assertThat(noKey.statusCode()).isEqualTo(409);
        JsonNode problem = TestHttp.JSON.readTree(noKey.body());
        assertThat(problem.get("title").asString()).isEqualTo("Builder not ready: Claude Code");
        assertThat(problem.get("detail").asString()).contains("Anthropic API key");

        http.json("PUT", "/api/builders/claude_code/auth", "{\"mode\":\"machine_sign_in\"}");
        HttpResponse<String> signedOut = http.call("POST", "/api/projects/" + project + "/runs", null);

        assertThat(signedOut.statusCode()).isEqualTo(409);
        assertThat(TestHttp.JSON.readTree(signedOut.body()).get("detail").asString())
                .contains("Run `claude` once in a terminal");
    }

    @Test
    void theBuilderStateTellsPerBuilderWhatIsMissingAndTheSignInModeIsChosenAtRunTime() throws Exception {
        JsonNode state = http.json("GET", "/api/builders", null);

        assertThat(state.get("demo").asBoolean()).isFalse();
        JsonNode claude = builder(state, "claude_code");
        assertThat(claude.get("installed").asBoolean()).isTrue();
        assertThat(claude.get("version").asString()).contains("9.9.9");
        assertThat(claude.get("authMode").asString()).isEqualTo("anthropic_key");
        assertThat(claude.get("ready").asBoolean()).isFalse();
        assertThat(claude.get("missing").get(0).get("code").asString()).isEqualTo("no_anthropic_key");
        JsonNode gemini = builder(state, "gemini_cli");
        assertThat(gemini.get("installed").asBoolean()).isTrue();
        assertThat(gemini.get("version").asString()).isEqualTo("0.0.0-fake");
        assertThat(gemini.get("missing").get(0).get("code").asString()).isEqualTo("no_gemini_key");

        JsonNode out = builder(http.json("PUT", "/api/builders/claude_code/auth",
                "{\"mode\":\"machine_sign_in\"}"), "claude_code");
        assertThat(out.get("authMode").asString()).isEqualTo("machine_sign_in");
        assertThat(out.get("signedIn").asString()).isEqualTo("no");
        assertThat(out.get("missing").get(0).get("code").asString()).isEqualTo("not_signed_in");
        assertThat(out.get("missing").get(0).get("instructionKey").asString())
                .isEqualTo("builders.claude.signInInstruction");

        Files.writeString(BIN.resolve("logged-in"), "yes");
        JsonNode signedIn = builder(http.json("GET", "/api/builders?refresh=true", null), "claude_code");
        assertThat(signedIn.get("signedIn").asString()).isEqualTo("yes");
        assertThat(signedIn.get("ready").asBoolean()).isTrue();

        setGeminiKey();
        assertThat(builder(http.json("GET", "/api/builders", null), "gemini_cli").get("ready").asBoolean())
                .isTrue();
        assertThat(http.call("PUT", "/api/builders/claude_code/auth", "{\"mode\":\"nonsense\"}")
                .statusCode()).isEqualTo(400);
    }

    @Test
    void theBuilderIsReadAndSetOnTheProject() throws Exception {
        UUID project = http.answeredProject("A canteen app");

        assertThat(http.json("GET", "/api/projects/" + project, null).get("agentHarness").asString())
                .isEqualTo("claude_code");
        JsonNode changed = http.json("PATCH", "/api/projects/" + project, "{\"agentHarness\":\"gemini_cli\"}");
        assertThat(changed.get("agentHarness").asString()).isEqualTo("gemini_cli");
        assertThat(http.call("PATCH", "/api/projects/" + project, "{\"agentHarness\":\"opencode\"}")
                .statusCode()).isEqualTo(400);
        assertThat(http.json("GET", "/api/projects/" + project, null).get("agentHarness").asString())
                .isEqualTo("gemini_cli");
    }

    @Test
    void aFailingStepGoesBackToTheBuilderWithTheFailureAndTheFixAttemptPasses() throws Exception {
        setGeminiKey();
        queue("ok", "fail", "ok");
        UUID project = confirmedProject("gemini_cli");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        JsonNode build = run.get("tasks").get(0).get("steps").get(1);
        assertThat(build.get("attempts").asInt()).isEqualTo(2);
        List<String> calls = Files.readAllLines(WORK.resolve("fake-gemini/args.log"));
        assertThat(calls.get(2)).contains("Your previous attempt at this step failed")
                .contains("The step went wrong: no luck");
    }

    @Test
    void aFailedCheckAfterAGeminiStepIsFixedInASecondInvocation() throws Exception {
        setGeminiKey();
        queue("ok", "badcheck", "ok");
        UUID project = confirmedProject("gemini_cli");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("tasks").get(0).get("steps").get(1).get("attempts").asInt()).isEqualTo(2);
        assertThat(Files.readAllLines(WORK.resolve("fake-gemini/args.log")).get(2))
                .contains("tests failed: 1");
    }

    @Test
    void aRateLimitOfTheQuotaMakesTheRunWaitAndTryAgainInsteadOfFailing() throws Exception {
        setGeminiKey();
        queue("limit", "limit");
        UUID project = confirmedProject("gemini_cli");

        String runId = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        JsonNode run = awaitEnd(runId);

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("tasks").get(0).get("steps").get(0).get("attempts").asInt())
                .as("two limited invocations and the one that worked").isEqualTo(3);
        List<String> messages = http.json("GET", "/api/runs/" + runId + "/events", null).valueStream()
                .map(e -> e.get("text").asString()).toList();
        assertThat(messages).anyMatch(m -> m.startsWith("waiting: Waiting for tokens"))
                .anyMatch(m -> m.contains("RESOURCE_EXHAUSTED"));
        assertThat(messages).noneMatch(m -> m.startsWith("Fix attempt"));
    }

    @Test
    void aResumeUsesTheBuilderTheRunStartedWithEvenWhenTheProjectChangedItsChoice() throws Exception {
        setGeminiKey();
        queue("fail", "fail", "fail");
        UUID project = confirmedProject("gemini_cli");
        String runId = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        JsonNode waiting = awaitEnd(runId);
        assertThat(waiting.get("status").asString()).isEqualTo("waiting");
        assertThat(waiting.get("waitKind").asString()).isEqualTo("question");

        http.json("PATCH", "/api/projects/" + project, "{\"agentHarness\":\"claude_code\"}");
        int before = Files.readAllLines(WORK.resolve("fake-gemini/args.log")).size();
        HttpResponse<String> answered = http.call("POST", "/api/runs/" + runId + "/answer",
                "{\"questionId\":\"" + waiting.get("question").get("id").asString()
                        + "\",\"choice\":\"retry\"}");

        assertThat(answered.statusCode()).as(answered.body()).isEqualTo(200);
        JsonNode run = awaitEnd(runId);
        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("harness").asString()).isEqualTo("gemini_cli");
        assertThat(Files.readAllLines(WORK.resolve("fake-gemini/args.log")).size()).isGreaterThan(before);
    }

    private void setGeminiKey() throws Exception {
        http.json("PUT", "/api/ai/keys/openai", "{\"apiKey\":\"" + GEMINI_KEY + "\",\"baseUrl\":\""
                + GEMINI_URL + "\",\"model\":\"gemini-flash-lite-latest\"}");
    }

    private static void queue(String... modes) throws IOException {
        Path dir = Files.createDirectories(WORK.resolve("fake-gemini"));
        Files.writeString(dir.resolve("queue"), String.join("\n", modes) + "\n");
    }

    private static JsonNode builder(JsonNode state, String id) {
        return state.get("builders").valueStream().filter(b -> id.equals(b.get("id").asString()))
                .findFirst().orElseThrow();
    }

    private UUID confirmedProject(String harness) throws Exception {
        UUID id = http.answeredProject("A canteen app");
        assertThat(http.call("POST", "/api/projects/" + id + "/requirements", null).statusCode())
                .isEqualTo(200);
        assertThat(http.call("POST", "/api/projects/" + id + "/workflow", null).statusCode())
                .isEqualTo(200);
        assertThat(http.call("POST", "/api/projects/" + id + "/workflow/confirm", null).statusCode())
                .isEqualTo(201);
        http.json("PATCH", "/api/projects/" + id, "{\"agentHarness\":\"" + harness + "\"}");
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

    /** The run as the API shows it, its events, and every log file and file of the run. */
    private String everythingTheEngineShows(JsonNode run) throws Exception {
        String id = run.get("id").asString();
        StringBuilder all = new StringBuilder(run.toString())
                .append(http.call("GET", "/api/runs/" + id + "/events", null).body())
                .append(http.call("GET", "/api/runs/" + id + "/report", null).body());
        Path workspace = WORK.resolve("run-" + id);
        try (Stream<Path> files = Stream.concat(Files.walk(workspace), Files.walk(
                WORK.resolve("run-" + id + "-logs")))) {
            for (Path file : files.filter(Files::isRegularFile).filter(f -> !f.toString().contains("/.git/"))
                    .toList()) {
                all.append(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1));
            }
        }
        return all.toString();
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
        for (String name : List.of("gemini", "claude")) {
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
