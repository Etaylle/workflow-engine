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
 * Runs through HTTP and the real PostgreSQL with the real Codex CLI builder class, but a fake
 * {@code codex} program (src/test/resources/fake-cli). No test calls OpenAI.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "engine.execution.templates-dir=../templates",
        "engine.execution.create-database=false", "engine.execution.limit-wait-base-ms=20",
        "engine.execution.limit-wait-max-ms=80", "engine.execution.limit-max-retries=3"})
@Import(ScriptedAi.class)
class CodexRunIT {

    private static final String GEMINI_KEY = "AIzaSyFAKE-test-key-0123456789abcdef";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/openai/";
    private static final Path BIN = fakePrograms();
    private static final Path WORK = tempDir("engine-codex-it-");

    @DynamicPropertySource
    static void fakeCommands(DynamicPropertyRegistry registry) {
        registry.add("engine.codex.cmd", () -> BIN.resolve("codex").toString());
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
        Path state = BIN.resolve("fake-codex");
        for (String file : List.of("queue", "args.log", "env.log")) {
            Files.deleteIfExists(state.resolve(file));
        }
    }

    @Test
    void aCodexRunBuildsWithTheMachineSignInRecordsTokensAndCostsNothing() throws Exception {
        signIn();
        setGeminiKey();
        UUID project = confirmedProject("codex_cli");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("harness").asString()).isEqualTo("codex_cli");
        assertThat(run.get("builderModel").asString()).isEqualTo("gpt-6.1-sol");
        assertThat(run.get("costUsd").asDouble()).as("ChatGPT plan: cost 0").isZero();
        JsonNode step = run.get("tasks").get(0).get("steps").get(0);
        assertThat(step.get("tokens").asLong()).isEqualTo(1234);
        assertThat(step.get("turns").asInt()).isEqualTo(3);
        assertThat(Files.readString(BIN.resolve("fake-codex/args.log"))).contains("exec --json")
                .contains("-s workspace-write").contains("AGENTS.md").doesNotContain(GEMINI_KEY);
        assertThat(Files.readString(BIN.resolve("fake-codex/env.log"))).contains("openai= gemini= anthropic=")
                .doesNotContain(GEMINI_KEY);
        assertThat(everythingTheEngineShows(run)).doesNotContain(GEMINI_KEY);
        assertThat(http.json("GET", "/api/runs/" + run.get("id").asString() + "/report", null)
                .get("harness").asString()).isEqualTo("codex_cli");
    }

    @Test
    void notSignedInIsAClear409NamingTheBuilderAndTheLoginCommand() throws Exception {
        UUID project = confirmedProject("codex_cli");

        HttpResponse<String> response = http.call("POST", "/api/projects/" + project + "/runs", null);

        assertThat(response.statusCode()).isEqualTo(409);
        JsonNode problem = TestHttp.JSON.readTree(response.body());
        assertThat(problem.get("title").asString()).isEqualTo("Builder not ready: Codex CLI");
        assertThat(problem.get("detail").asString()).contains(BIN.resolve("codex") + " login")
                .doesNotContain("Anthropic").doesNotContain("Gemini");
    }

    @Test
    void theBuilderStateShowsInstalledSignedInAndTheModelsAndEfforts() throws Exception {
        JsonNode out = builder(http.json("GET", "/api/builders", null), "codex_cli");

        assertThat(out.get("installed").asBoolean()).isTrue();
        assertThat(out.get("version").asString()).isEqualTo("codex-cli 0.0.0-fake");
        assertThat(out.get("signedIn").asString()).isEqualTo("no");
        assertThat(out.get("ready").asBoolean()).isFalse();
        JsonNode missing = out.get("missing").get(0);
        assertThat(missing.get("code").asString()).isEqualTo("codex_not_signed_in");
        assertThat(missing.get("instructionKey").asString()).isEqualTo("builders.codex.signInInstruction");
        assertThat(out.get("models").valueStream().map(m -> m.get("id").asString()).toList())
                .containsExactly("gpt-6-luna", "gpt-6.1-sol", "gpt-6-astra");
        assertThat(out.get("efforts").valueStream().map(JsonNode::asString).toList())
                .containsExactly("low", "medium", "high", "xhigh");
        assertThat(out.get("customModel").asBoolean()).isTrue();

        signIn();
        JsonNode in = builder(http.json("GET", "/api/builders?refresh=true", null), "codex_cli");
        assertThat(in.get("signedIn").asString()).isEqualTo("yes");
        assertThat(in.get("ready").asBoolean()).isTrue();
    }

    @Test
    void aFailingStepGoesBackToTheBuilderWithTheFailureAndTheFixAttemptPasses() throws Exception {
        signIn();
        queue("ok", "fail", "ok");
        UUID project = confirmedProject("codex_cli");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("tasks").get(0).get("steps").get(1).get("attempts").asInt()).isEqualTo(2);
        List<String> calls = Files.readAllLines(BIN.resolve("fake-codex/args.log"));
        assertThat(calls.get(2)).contains("Your previous attempt at this step failed")
                .contains("The step went wrong: no luck");
    }

    @Test
    void aUsageLimitMakesTheRunWaitAndTryAgainInsteadOfFailing() throws Exception {
        signIn();
        queue("limit", "limit");
        UUID project = confirmedProject("codex_cli");

        String runId = http.json("POST", "/api/projects/" + project + "/runs", null).get("id").asString();
        JsonNode run = awaitEnd(runId);

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("tasks").get(0).get("steps").get(0).get("attempts").asInt())
                .as("two limited invocations and the one that worked").isEqualTo(3);
        List<String> messages = http.json("GET", "/api/runs/" + runId + "/events", null).valueStream()
                .map(e -> e.get("text").asString()).toList();
        assertThat(messages).anyMatch(m -> m.startsWith("waiting: Waiting for tokens"))
                .anyMatch(m -> m.contains("usage limit"));
        assertThat(messages).noneMatch(m -> m.startsWith("Fix attempt"));
    }

    @Test
    void theChosenModelAndEffortAreSnapshottedAndPassedAsArguments() throws Exception {
        signIn();
        UUID project = confirmedProject("codex_cli");
        http.json("PATCH", "/api/projects/" + project, "{\"builderModel\":\"gpt-6-luna\",\"builderEffort\":\"low\"}");

        JsonNode run = awaitEnd(http.json("POST", "/api/projects/" + project + "/runs", null)
                .get("id").asString());

        assertThat(run.get("status").asString()).isEqualTo("done");
        assertThat(run.get("builderModel").asString()).isEqualTo("gpt-6-luna");
        assertThat(run.get("builderEffort").asString()).isEqualTo("low");
        assertThat(Files.readString(BIN.resolve("fake-codex/args.log"))).contains("-m gpt-6-luna")
                .contains("model_reasoning_effort=\"low\"");
    }

    @Test
    void theModelAndEffortAreCheckedForCodexAndClearedWhenTheBuilderChanges() throws Exception {
        UUID project = http.answeredProject("A canteen app");
        String url = "/api/projects/" + project;

        JsonNode codex = http.json("PATCH", url, "{\"agentHarness\":\"codex_cli\",\"builderModel\":\"gpt-6-astra\","
                + "\"builderEffort\":\"xhigh\"}");
        assertThat(codex.get("agentHarness").asString()).isEqualTo("codex_cli");
        assertThat(codex.get("builderModel").asString()).isEqualTo("gpt-6-astra");
        assertThat(codex.get("builderEffort").asString()).isEqualTo("xhigh");
        HttpResponse<String> max = http.call("PATCH", url, "{\"builderEffort\":\"max\"}");
        assertThat(max.statusCode()).isEqualTo(400);
        assertThat(max.body()).contains("low, medium, high, xhigh");
        assertThat(http.call("PATCH", url, "{\"builderModel\":\"bad model!\"}").statusCode()).isEqualTo(400);

        JsonNode gemini = http.json("PATCH", url, "{\"agentHarness\":\"gemini_cli\"}");
        assertThat(gemini.get("builderModel").isNull()).isTrue();
        assertThat(gemini.get("builderEffort").isNull()).isTrue();
    }

    private static void signIn() throws IOException {
        Files.writeString(BIN.resolve("codex-logged-in"), "yes");
    }

    private void setGeminiKey() throws Exception {
        http.json("PUT", "/api/ai/keys/openai", "{\"apiKey\":\"" + GEMINI_KEY + "\",\"baseUrl\":\""
                + GEMINI_URL + "\",\"model\":\"gemini-flash-lite-latest\"}");
    }

    private static void queue(String... modes) throws IOException {
        Path dir = Files.createDirectories(BIN.resolve("fake-codex"));
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
        for (String name : List.of("codex")) {
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
