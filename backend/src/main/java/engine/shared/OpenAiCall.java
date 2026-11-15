package engine.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.ObjectMappers;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One planning call to an OpenAI-compatible provider (OpenAI, Gemini, Groq, OpenRouter, ...):
 * strict structured output by class; when the provider does not accept the strict schema, one
 * retry in JSON mode with the schema in the prompt.
 */
public final class OpenAiCall {

    private static final Duration TIMEOUT = Duration.ofSeconds(300);
    private static final ObjectMapper JSON = ObjectMappers.jsonMapper();
    private static final Pattern RETRY_IN =
            Pattern.compile("retry in ([0-9]+(?:\\.[0-9]+)?)s|retryDelay\\W+([0-9]+)s", Pattern.CASE_INSENSITIVE);

    private OpenAiCall() {
    }

    /** Asks the provider one question and reads the answer as the given class. */
    static <T> T structured(AiTarget target, String system, String user, Class<T> type) {
        OpenAIClient client = client(target);
        try {
            try {
                return answer(client.chat().completions().create(params(target, system, user, type)));
            } catch (OpenAIServiceException e) {
                if (!rejectsStrictSchema(e)) {
                    throw e;
                }
            }
            return jsonMode(client, target, system, user, type);
        } catch (OpenAIInvalidDataException e) {
            throw new InvalidAiOutputException("The answer was not valid JSON for the schema.", e);
        } catch (OpenAIException e) {
            throw problem(e, target);
        }
    }

    /** The client of one call; a base URL of the session was checked when it was set. */
    public static OpenAIClient client(AiTarget target) {
        return OpenAIOkHttpClient.builder()
                .apiKey(target.apiKey())
                .baseUrl(target.baseUrl())
                .timeout(TIMEOUT)
                .maxRetries(0)
                .build();
    }

    /** The strict request: the schema is derived from the class and checked locally. */
    static <T> StructuredChatCompletionCreateParams<T> params(
            AiTarget target, String system, String user, Class<T> type) {
        return ChatCompletionCreateParams.builder()
                .model(target.model())
                .addSystemMessage(system)
                .addUserMessage(user)
                .responseFormat(type)
                .build();
    }

    /** The JSON-mode request: the schema of the class goes into the prompt. */
    static <T> ChatCompletionCreateParams jsonModeParams(
            AiTarget target, String system, String user, Class<T> type) {
        String schema = schemaOf(params(target, system, user, type));
        return ChatCompletionCreateParams.builder()
                .model(target.model())
                .addSystemMessage(system + "\n\nAnswer with one JSON object only, no other text,"
                        + " that matches this JSON Schema:\n" + schema)
                .addUserMessage(user)
                .responseFormat(ResponseFormatJsonObject.builder().build())
                .build();
    }

    /** The JSON text of the schema the SDK derived from the class for the strict request. */
    @SuppressWarnings("deprecation") // the SDK offers no other way to read the derived schema
    static <T> String schemaOf(StructuredChatCompletionCreateParams<T> strict) {
        var field = strict.rawParams().responseFormat().orElseThrow().asJsonSchema().jsonSchema()._schema();
        Object schema = field.asKnown().<Object>map(known -> known._additionalProperties())
                .orElseGet(() -> field.asUnknown().orElseThrow());
        try {
            return JSON.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The schema cannot be written as JSON.", e);
        }
    }

    private static <T> T jsonMode(
            OpenAIClient client, AiTarget target, String system, String user, Class<T> type) {
        ChatCompletion completion =
                client.chat().completions().create(jsonModeParams(target, system, user, type));
        String text = checkedText(completion.choices().stream().findFirst().orElse(null), target);
        try {
            return JSON.readValue(stripFence(text), type);
        } catch (JsonProcessingException e) {
            throw new InvalidAiOutputException("The answer was not valid JSON for the schema.", e);
        }
    }

    private static String stripFence(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstLine = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (firstLine > 0 && end > firstLine) {
                return trimmed.substring(firstLine + 1, end).strip();
            }
        }
        return trimmed;
    }

    /** Reads a finished strict call: a refusal or a cut-off answer is a problem, not a retry. */
    static <T> T answer(StructuredChatCompletion<T> completion) {
        // The stop reason is read from the raw answer first: a cut-off answer is not valid JSON.
        var raw = completion.rawChatCompletion().choices().stream().findFirst().orElse(null);
        AiTarget named = new AiTarget(AiProvider.OPENAI, "", "", completion.model());
        String text = checkedText(raw, named);
        if (text.isBlank()) {
            throw new InvalidAiOutputException("The provider returned an empty answer.");
        }
        return completion.choices().get(0).message().content().orElseThrow();
    }

    private static String checkedText(ChatCompletion.Choice choice, AiTarget target) {
        String finish = choice == null ? null : choice.finishReason().asString();
        String refusal = choice == null ? null : choice.message().refusal().orElse(null);
        String text = choice == null ? null : choice.message().content().orElse(null);
        if (text == null || refusal != null || "length".equals(finish) || "content_filter".equals(finish)) {
            throw stopped(refusal, finish, target);
        }
        return text;
    }

    /** An answer that is not an answer: refusal, safety block, no candidate, or cut off. */
    private static RuntimeException stopped(String refusal, String finish, AiTarget target) {
        String who = target.label();
        if ("length".equals(finish)) {
            return new ApiProblem(502, "The answer was cut off",
                    "The answer of " + who + " was cut off by the provider's output limit. "
                            + "Shorten the wish or the answers and try again.");
        }
        if (refusal != null || finish == null || "content_filter".equals(finish)) {
            return new ApiProblem(422, "The AI declined",
                    who + " declined or blocked the answer (safety filter or empty answer)."
                            + " Reword the wish and try again.");
        }
        return new InvalidAiOutputException("The provider returned no answer (finish reason " + finish + ").");
    }

    /** True when the provider says it cannot take the strict json_schema response format. */
    static boolean rejectsStrictSchema(OpenAIServiceException e) {
        if (e.statusCode() != 400 && e.statusCode() != 422) {
            return false;
        }
        String text = (String.valueOf(e.getMessage()) + " " + e.code().orElse("")).toLowerCase(Locale.ROOT);
        return text.contains("response_format") || text.contains("json_schema")
                || text.contains("structured") || text.contains("strict")
                || text.contains("schema") || text.contains("additionalproperties");
    }

    /** Maps a failed call to the problem the page shows; never contains the key. */
    public static ApiProblem problem(OpenAIException e, AiTarget target) {
        String who = target.label();
        if (!(e instanceof OpenAIServiceException service)) {
            return new ApiProblem(504, who + " not reachable",
                    who + " did not answer in time or could not be reached ("
                            + e.getClass().getSimpleName() + "). Check the base URL and try again."
                            + ("Gemini".equals(who)
                                    ? " Some Gemini models are slow or unavailable on the free tier;"
                                            + " try the model gemini-flash-lite-latest."
                                    : ""), e);
        }
        String reason = redact(service.getMessage(), target.apiKey());
        String lower = (reason + " " + service.code().orElse("")).toLowerCase(Locale.ROOT);
        int status = service.statusCode();
        if (lower.contains("insufficient_quota") || lower.contains("no credits remaining")) {
            return new ApiProblem(429, who + " credit used up",
                    "The " + who + " account has no credit left. Add credit with " + who
                            + " (billing page of the provider) or use another key, then try again.", e);
        }
        if (lower.contains("api_key_invalid") || lower.contains("api key not valid")
                || status == 401) {
            return new ApiProblem(401, who + " key rejected",
                    who + " rejected the key. Enter it again.", e);
        }
        return switch (status) {
            case 403 -> new ApiProblem(403, who + " refused access",
                    "The key may not use the model " + target.model() + ". " + reason, e);
            case 404 -> new ApiProblem(502, "Model not available",
                    who + " does not know the model " + target.model()
                            + " (or the base URL is wrong): " + reason
                            + " Change the model or the base URL in the key panel.", e);
            case 429 -> new ApiProblem(429, "Rate limit reached", rateLimit(who, service, reason), e);
            case 500, 502, 503, 504 -> new ApiProblem(503, who + " overloaded",
                    who + " is busy or down (status " + status + "). Try again in a minute.", e);
            default -> new ApiProblem(502, who + " call failed",
                    who + " refused the request (status " + status + "): " + reason, e);
        };
    }

    private static String rateLimit(String who, OpenAIServiceException e, String reason) {
        Matcher match = RETRY_IN.matcher(reason);
        String seconds = null;
        if (match.find()) {
            seconds = match.group(1) != null ? match.group(1) : match.group(2);
        } else {
            seconds = e.headers().values("retry-after").stream().findFirst().orElse(null);
        }
        if (seconds == null) {
            return "The limit of " + who + " is reached (free keys have a small limit per minute"
                    + " and per day). Try again in a minute.";
        }
        long wait;
        try {
            wait = (long) Math.ceil(Double.parseDouble(seconds));
        } catch (NumberFormatException ignored) {
            return "The limit of " + who + " is reached. Try again in a minute.";
        }
        return "The limit of " + who + " is reached (free keys have a small limit per minute and per"
                + " day). Try again in " + wait + " seconds.";
    }

    private static String redact(String text, String apiKey) {
        String safe = text == null ? "" : text;
        return apiKey == null || apiKey.isBlank() ? safe : safe.replace(apiKey, "[key]");
    }
}
