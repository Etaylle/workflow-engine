package engine.shared;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import java.time.Duration;
import java.util.List;

/** One planning call to Claude: a client made from the session key, structured output by class. */
public final class AnthropicCall {

    /** Standard planning model (D-27). */
    public static final String MODEL = "claude-sonnet-5-5";

    private static final Duration TIMEOUT = Duration.ofSeconds(300);
    private static final long MAX_TOKENS = 20_000L;

    private AnthropicCall() {
    }

    /**
     * Asks Claude one question and reads the answer as the given class.
     *
     * @param apiKey the user's Anthropic key for this session
     * @param system instructions of the call
     * @param user material of the call (wish, answers, correction)
     * @param type class the structured answer is read into
     * @throws ApiProblem when the call fails (key, rate limit, credit, time, service, refusal)
     * @throws InvalidAiOutputException when the answer cannot be read as the class
     */
    public static <T> T structured(String apiKey, String system, String user, Class<T> type) {
        AnthropicClient client =
                AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .timeout(TIMEOUT)
                        .maxRetries(0)
                        .build();
        StructuredMessageCreateParams<T> params = params(system, user, type);
        try {
            return answer(client.messages().create(params));
        } catch (AnthropicInvalidDataException e) {
            throw new InvalidAiOutputException("The answer was not valid JSON for the schema.", e);
        } catch (AnthropicException e) {
            throw problem(e, apiKey);
        }
    }

    /** The request of a planning call; the schema of the class is derived and checked locally. */
    public static <T> StructuredMessageCreateParams<T> params(
            String system, String user, Class<T> type) {
        return MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .system(system)
                .outputConfig(type)
                .addUserMessage(user)
                .build();
    }

    /**
     * Reads the answer of a finished call: a refusal or a cut-off answer is a problem of its own,
     * not an invalid answer to retry.
     */
    public static <T> T answer(StructuredMessage<T> message) {
        StopReason stop = message.stopReason().orElse(null);
        if (StopReason.REFUSAL.equals(stop)) {
            throw new ApiProblem(422, "Claude declined",
                    "Claude declined to answer this wish. Reword the wish and try again.");
        }
        if (StopReason.MAX_TOKENS.equals(stop)) {
            throw new ApiProblem(502, "Claude's answer was cut off",
                    "Claude's answer did not fit into " + MAX_TOKENS + " tokens. "
                            + "Shorten the wish or the answers and try again.");
        }
        List<T> answers =
                message.content().stream()
                        .flatMap(block -> block.text().stream())
                        .map(typed -> typed.text())
                        .toList();
        if (answers.isEmpty()) {
            throw new InvalidAiOutputException("Claude returned no answer (stop reason "
                    + (stop == null ? "unknown" : stop.toString()) + ").");
        }
        return answers.get(0);
    }

    /**
     * Maps a failed call to the problem the page shows. Never contains the key: the detail is
     * fixed text plus the API's own message with the key blanked out.
     */
    public static ApiProblem problem(AnthropicException e, String apiKey) {
        if (!(e instanceof AnthropicServiceException service)) {
            return new ApiProblem(504, "Anthropic not reachable",
                    "Anthropic did not answer in time or could not be reached ("
                            + e.getClass().getSimpleName() + "). Try again.", e);
        }
        String reason = redact(service.getMessage(), apiKey);
        int status = service.statusCode();
        if (reason.toLowerCase(java.util.Locale.ROOT).contains("credit balance")) {
            return new ApiProblem(429, "Anthropic credit used up",
                    "The credit of the Anthropic account is used up. Add credit and try again.", e);
        }
        return switch (status) {
            case 401 -> new ApiProblem(401, "Anthropic key rejected",
                    "Anthropic rejected the key. Enter it again.", e);
            case 402 -> new ApiProblem(429, "Anthropic credit used up",
                    "The billing of the Anthropic account needs attention. Fix it and try again.", e);
            case 403 -> new ApiProblem(403, "Anthropic refused access",
                    "The key may not use this model or service. Check the account.", e);
            case 404 -> new ApiProblem(502, "Model not available",
                    "Anthropic does not know or allow the model " + MODEL + " for this key.", e);
            case 429 -> new ApiProblem(429, "Rate limit reached",
                    "Anthropic's rate limit is reached. Try again later.", e);
            case 500, 502, 503, 504, 529 -> new ApiProblem(503, "Anthropic overloaded",
                    "Anthropic is busy or down (status " + status + "). Try again in a minute.", e);
            default -> new ApiProblem(502, "Anthropic call failed",
                    "Anthropic refused the request (status " + status + "): " + reason, e);
        };
    }

    private static String redact(String text, String apiKey) {
        String safe = text == null ? "" : text;
        return apiKey == null || apiKey.isBlank() ? safe : safe.replace(apiKey, "[key]");
    }
}
