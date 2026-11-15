package engine.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.InternalServerException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.errors.UnexpectedStatusCodeException;
import com.anthropic.core.JsonValue;
import com.anthropic.core.http.Headers;
import org.junit.jupiter.api.Test;

class AnthropicCallTest {

    private record Pair(String a, int b) {
    }

    private static final String KEY = "sk-ant-secret-123";

    @Test
    void requestUsesTheStandardModelAndFitsTheTimeouts() {
        var params = AnthropicCall.params("system", "user", Pair.class);

        assertThat(params.model().asString()).isEqualTo("claude-sonnet-5-5");
        assertThat(params.maxTokens()).isEqualTo(20_000L);
        assertThat(params.thinking()).as("no thinking parameter: Sonnet 5.5 rejects budget_tokens").isEmpty();
        assertThat(params.rawParams()._additionalBodyProperties()).doesNotContainKeys("temperature", "top_p", "top_k");
        assertThat(params.outputConfig()).isPresent();
    }

    @Test
    void finishedAnswerIsReadAsTheClass() {
        assertThat(ClaudeAnswers.read(Pair.class, "{\"a\":\"x\",\"b\":2}")).isEqualTo(new Pair("x", 2));
    }

    @Test
    void refusalIsAProblemNotARetry() {
        var message = ClaudeAnswers.message(Pair.class, "{}", "refusal");

        assertThatThrownBy(() -> AnthropicCall.answer(message))
                .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(422));
    }

    @Test
    void cutOffAnswerIsAProblemNotARetry() {
        var message = ClaudeAnswers.message(Pair.class, "{\"a\":\"x", "max_tokens");

        assertThatThrownBy(() -> AnthropicCall.answer(message))
                .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(502));
    }

    @Test
    void invalidJsonSurfacesAsInvalidDataForTheRetry() {
        assertThatThrownBy(() -> ClaudeAnswers.read(Pair.class, "not json"))
                .isInstanceOf(com.anthropic.errors.AnthropicInvalidDataException.class);
    }

    @Test
    void statusCodesMapToProblems() {
        assertThat(status(UnauthorizedException.builder().headers(Headers.builder().build()).body(JsonValue.from("x")).build())).isEqualTo(401);
        assertThat(status(RateLimitException.builder().headers(Headers.builder().build()).body(JsonValue.from("x")).build())).isEqualTo(429);
        assertThat(status(InternalServerException.builder().statusCode(529).headers(Headers.builder().build()).body(JsonValue.from("x")).build())).isEqualTo(503);
        assertThat(status(UnexpectedStatusCodeException.builder().statusCode(402).headers(Headers.builder().build()).body(JsonValue.from("x")).build())).isEqualTo(429);
        assertThat(status(new AnthropicIoException("timeout"))).isEqualTo(504);
    }

    @Test
    void exhaustedCreditAnswered400IsReadAsCredit() {
        var e = BadRequestException.builder().headers(Headers.builder().build())
                .body(JsonValue.from("Your credit balance is too low to access the Anthropic API")).build();

        assertThat(AnthropicCall.problem(e, KEY).title()).isEqualTo("Anthropic credit used up");
    }

    @Test
    void keyNeverReachesTheProblemText() {
        var e = BadRequestException.builder().headers(Headers.builder().build())
                .body(JsonValue.from("bad request for " + KEY)).build();

        ApiProblem problem = AnthropicCall.problem(e, KEY);

        assertThat(problem.getMessage()).doesNotContain(KEY);
        assertThat(problem.title()).doesNotContain(KEY);
    }

    private static int status(com.anthropic.errors.AnthropicException e) {
        return AnthropicCall.problem(e, KEY).status();
    }
}
