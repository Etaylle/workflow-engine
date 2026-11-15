package engine.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.ObjectMappers;
import com.openai.models.ErrorObject;
import com.openai.core.http.Headers;
import com.openai.errors.BadRequestException;
import com.openai.errors.InternalServerException;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCallTest {

    private record Pair(String a, int b) {
    }

    private static final String KEY = "AQ.secret-key-123";
    private static final ObjectMapper JSON = ObjectMappers.jsonMapper();

    private HttpServer server;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** A fake provider: answers request n (from 0) with the status and body of the function. */
    private AiTarget provider(Function<Integer, Map.Entry<Integer, String>> answers) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<Integer> count = new ArrayList<>();
        server.createContext("/v1/chat/completions", exchange -> {
            requests.add(JSON.readTree(exchange.getRequestBody().readAllBytes()));
            var answer = answers.apply(count.size());
            count.add(1);
            byte[] body = answer.getValue().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(answer.getKey(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return new AiTarget(AiProvider.OPENAI, KEY,
                "http://localhost:" + server.getAddress().getPort() + "/v1", "test-model");
    }

    private static String completion(String content, String finish) throws Exception {
        return JSON.writeValueAsString(Map.of(
                "id", "chatcmpl-1", "object", "chat.completion", "created", 1, "model", "test-model",
                "choices", List.of(Map.of("index", 0, "finish_reason", finish,
                        "message", Map.of("role", "assistant", "content", content))),
                "usage", Map.of("prompt_tokens", 5, "completion_tokens", 5, "total_tokens", 10)));
    }

    private static String error(String message, String code) throws Exception {
        return JSON.writeValueAsString(Map.of("error", Map.of("message", message, "code", code)));
    }

    @Test
    void strictRequestCarriesTheSchemaAndNoExtraParameters() throws Exception {
        AiTarget target = provider(n -> Map.entry(200, safe(() -> completion("{\"a\":\"x\",\"b\":2}", "stop"))));

        Pair result = OpenAiCall.structured(target, "system", "user", Pair.class);

        assertThat(result).isEqualTo(new Pair("x", 2));
        JsonNode sent = requests.get(0);
        assertThat(sent.path("response_format").path("type").asText()).isEqualTo("json_schema");
        assertThat(sent.path("response_format").path("json_schema").path("strict").asBoolean()).isTrue();
        assertThat(sent.has("max_tokens")).isFalse();
        assertThat(sent.has("max_completion_tokens")).isFalse();
        assertThat(sent.path("model").asText()).isEqualTo("test-model");
    }

    @Test
    void providerThatRejectsTheStrictSchemaGetsOneRetryInJsonMode() throws Exception {
        AiTarget target = provider(n -> n == 0
                ? Map.entry(400, safe(() -> error("Unsupported response_format json_schema", "invalid_argument")))
                : Map.entry(200, safe(() -> completion("```json\n{\"a\":\"y\",\"b\":3}\n```", "stop"))));

        Pair result = OpenAiCall.structured(target, "system", "user", Pair.class);

        assertThat(result).isEqualTo(new Pair("y", 3));
        assertThat(requests).hasSize(2);
        JsonNode retry = requests.get(1);
        assertThat(retry.path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(retry.path("messages").get(0).path("content").asText())
                .contains("JSON Schema", "additionalProperties", "\"a\"");
    }

    @Test
    void jsonModeAnswerThatIsNotTheSchemaIsAnInvalidAnswerForTheRetry() throws Exception {
        AiTarget target = provider(n -> n == 0
                ? Map.entry(400, safe(() -> error("strict is not supported", "invalid_argument")))
                : Map.entry(200, safe(() -> completion("sorry, here is prose", "stop"))));

        assertThatThrownBy(() -> OpenAiCall.structured(target, "s", "u", Pair.class))
                .isInstanceOf(InvalidAiOutputException.class);
    }

    @Test
    void unrelatedBadRequestIsNotRetried() throws Exception {
        AiTarget target = provider(n -> Map.entry(400, safe(() -> error("The model is invalid", "invalid_model"))));

        assertThatThrownBy(() -> OpenAiCall.structured(target, "s", "u", Pair.class))
                .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(502));
        assertThat(requests).hasSize(1);
    }

    @Test
    void rateLimitSaysHowLongToWait() throws Exception {
        AiTarget target = provider(n -> Map.entry(429, safe(() -> error(
                "You exceeded your current quota, please check your plan and billing details. "
                        + "Please retry in 23.4s.", "RESOURCE_EXHAUSTED"))));

        assertThatThrownBy(() -> OpenAiCall.structured(target, "s", "u", Pair.class))
                .isInstanceOfSatisfying(ApiProblem.class, e -> {
                    assertThat(e.status()).isEqualTo(429);
                    assertThat(e.title()).isEqualTo("Rate limit reached");
                    assertThat(e.getMessage()).contains("24 seconds").doesNotContain(KEY);
                });
    }

    @Test
    void realisticOpenAiAnswerIsReadThroughTheSdk() throws Exception {
        String raw = completion("{\"a\":\"hello\",\"b\":7}", "stop");
        var completion = new StructuredChatCompletion<>(Pair.class, JSON.readValue(raw, ChatCompletion.class));

        assertThat(OpenAiCall.answer(completion)).isEqualTo(new Pair("hello", 7));
    }

    @Test
    void refusalSafetyBlockAndEmptyAnswerAreDeclined() throws Exception {
        String refusal = JSON.writeValueAsString(Map.of(
                "id", "c", "object", "chat.completion", "created", 1, "model", "m",
                "choices", List.of(Map.of("index", 0, "finish_reason", "stop",
                        "message", Map.of("role", "assistant", "content", "", "refusal", "I cannot help")))));
        String filtered = completion("", "content_filter");
        String empty = JSON.writeValueAsString(Map.of("id", "c", "object", "chat.completion", "created", 1,
                "model", "m", "choices", List.of()));
        for (String raw : List.of(refusal, filtered, empty)) {
            var completion = new StructuredChatCompletion<>(Pair.class, JSON.readValue(raw, ChatCompletion.class));
            assertThatThrownBy(() -> OpenAiCall.answer(completion))
                    .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(422));
        }
    }

    @Test
    void cutOffAnswerIsAProblemNotARetry() throws Exception {
        var completion = new StructuredChatCompletion<>(Pair.class,
                JSON.readValue(completion("{\"a\":\"x", "length"), ChatCompletion.class));

        assertThatThrownBy(() -> OpenAiCall.answer(completion))
                .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(502));
    }

    @Test
    void statusCodesMapToProblemsNamingTheProvider() {
        AiTarget openai = new AiTarget(AiProvider.OPENAI, KEY, "https://api.openai.com/v1", "gpt-5.4-mini");
        AiTarget gemini = new AiTarget(AiProvider.OPENAI, KEY,
                "https://generativelanguage.googleapis.com/v1beta/openai/", "gemini-flash-lite-latest");
        Headers none = Headers.builder().build();

        assertThat(OpenAiCall.problem(UnauthorizedException.builder().headers(none)
                .error(err("x", "x")).build(), openai).status()).isEqualTo(401);
        assertThat(OpenAiCall.problem(BadRequestException.builder().headers(none)
                .error(err("API key not valid. Please pass a valid API key.", "API_KEY_INVALID"))
                .build(), gemini).title()).isEqualTo("Gemini key rejected");
        assertThat(OpenAiCall.problem(InternalServerException.builder().statusCode(503).headers(none)
                .error(err("x", "x")).build(), openai).status()).isEqualTo(503);
        assertThat(OpenAiCall.problem(new OpenAIIoException("timeout"), gemini).getMessage())
                .contains("gemini-flash-lite-latest");
        var missing = OpenAiCall.problem(NotFoundException.builder().headers(none)
                .error(err("model not found", "model_not_found")).build(), gemini);
        assertThat(missing.title()).isEqualTo("Model not available");
        assertThat(missing.getMessage()).contains("gemini-flash-lite-latest", "Gemini");
    }

    @Test
    void exhaustedCreditNamesTheProviderWhoseCreditIsMissing() {
        AiTarget openai = new AiTarget(AiProvider.OPENAI, KEY, "https://api.openai.com/v1", "gpt-5.4-mini");
        Headers none = Headers.builder().build();
        var noCredits = RateLimitException.builder().headers(none).error(err("You have no credits remaining. Add credits to continue using the API at"
                        + " https://platform.openai.com/settings/organization/billing/.",
                "insufficient_quota")).build();
        var quota = RateLimitException.builder().headers(none).error(err("You exceeded your current quota", "insufficient_quota")).build();

        assertThat(OpenAiCall.problem(noCredits, openai).title()).isEqualTo("OpenAI credit used up");
        assertThat(OpenAiCall.problem(quota, openai).getMessage()).contains("OpenAI");
        assertThat(OpenAiCall.problem(quota, openai).status()).isEqualTo(429);
    }

    @Test
    void keyNeverReachesTheProblemText() {
        AiTarget target = new AiTarget(AiProvider.OPENAI, KEY, "https://api.openai.com/v1", "m");
        var e = BadRequestException.builder().headers(Headers.builder().build())
                .error(err("bad request for " + KEY, "bad")).build();

        ApiProblem problem = OpenAiCall.problem(e, target);

        assertThat(problem.getMessage()).doesNotContain(KEY);
        assertThat(problem.title()).doesNotContain(KEY);
        assertThat(target.toString()).doesNotContain(KEY);
    }

    private static ErrorObject err(String message, String code) {
        return ErrorObject.builder().message(message).type("error").code(code)
                .param(java.util.Optional.<String>empty()).build();
    }

    private interface Thrower {
        String get() throws Exception;
    }

    private static String safe(Thrower thrower) {
        try {
            return thrower.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
