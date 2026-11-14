package engine.shared;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/** One planning call to Claude: a client made from the session key, structured output by class. */
public final class ClaudeCall {

    /** Standard planning model (D-27). */
    public static final String MODEL = "claude-sonnet-5-5";

    private static final Duration TIMEOUT = Duration.ofSeconds(120);
    private static final long MAX_TOKENS = 16_000L;

    private ClaudeCall() {
    }

    /**
     * Asks Claude one question and reads the answer as the given class.
     *
     * @param apiKey the user's key for this session
     * @param system instructions of the call
     * @param user material of the call (wish, answers, correction)
     * @param type class the structured answer is read into
     * @throws ApiProblem when the call fails (key, rate limit, time, service)
     * @throws InvalidAiOutputException when the answer cannot be read as the class
     */
    public static <T> T structured(String apiKey, String system, String user, Class<T> type) {
        AnthropicClient client =
                AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .timeout(TIMEOUT)
                        .maxRetries(0)
                        .build();
        StructuredMessageCreateParams<T> params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(MAX_TOKENS)
                        .system(system)
                        .outputConfig(type)
                        .addUserMessage(user)
                        .build();
        try {
            StructuredMessage<T> message = client.messages().create(params);
            List<T> answers =
                    message.content().stream()
                            .flatMap(block -> block.text().stream())
                            .map(typed -> typed.text())
                            .toList();
            if (answers.isEmpty()) {
                throw new InvalidAiOutputException("Claude returned no answer (stop reason "
                        + message.stopReason().map(Object::toString).orElse("unknown") + ").");
            }
            return answers.get(0);
        } catch (UnauthorizedException e) {
            throw new ApiProblem(
                    401, "Anthropic key rejected", "Anthropic rejected the key. Enter it again.", e);
        } catch (RateLimitException e) {
            throw new ApiProblem(429, "Rate limit reached",
                    "Anthropic's rate limit or credit is used up. Try again later.", e);
        } catch (AnthropicInvalidDataException e) {
            throw new InvalidAiOutputException("The answer was not valid JSON for the schema.", e);
        } catch (AnthropicException e) {
            throw new ApiProblem(502, "Anthropic call failed",
                    "Anthropic did not complete the call (" + e.getClass().getSimpleName() + ").", e);
        }
    }

    /** The user message: the material, plus the reason of the last failure on the retry. */
    public static String withCorrection(String material, String correction) {
        return correction == null
                ? material
                : material + "\n\nYour previous answer was invalid: " + correction + "\nFix it.";
    }

    /**
     * Runs a call, validates the answer, and retries once with the reason when it is invalid.
     *
     * @param what name of the thing written, for the error message
     * @param call the call, given the reason of the first failure or null
     * @param validate throws {@link InvalidAiOutputException} with the reason when invalid
     */
    public static <T> T validated(
            String what, Function<String, T> call, java.util.function.Consumer<T> validate) {
        String correction = null;
        for (int attempt = 1; ; attempt++) {
            try {
                T result = call.apply(correction);
                validate.accept(result);
                return result;
            } catch (InvalidAiOutputException e) {
                if (attempt == 2) {
                    throw new ApiProblem(502, "Invalid " + what + " from Claude",
                            "Claude's " + what + " was invalid twice (" + e.getMessage()
                                    + "). Try again or reword the wish.", e);
                }
                correction = e.getMessage();
            }
        }
    }

    /** The key of the session, or a 409 problem telling the user to enter it. */
    public static String requireKey(SessionKey sessionKey) {
        return sessionKey.get().orElseThrow(() -> new ApiProblem(409, "Anthropic key missing",
                "No Anthropic key is set for this session. Enter your key and try again."));
    }
}
