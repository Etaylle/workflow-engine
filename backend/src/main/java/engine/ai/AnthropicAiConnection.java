package engine.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.MessageCreateParams;
import java.time.Duration;

/** Real check: one tiny message to Claude Haiku with a client made from the session key. */
public class AnthropicAiConnection implements AiConnection {

    static final String CHECK_MODEL = "claude-haiku-5-5";

    @Override
    public String check(String apiKey) {
        AnthropicClient client =
                AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .timeout(Duration.ofSeconds(30))
                        .maxRetries(0)
                        .build();
        try {
            client.messages()
                    .create(
                            MessageCreateParams.builder()
                                    .model(CHECK_MODEL)
                                    .maxTokens(1024L)
                                    .addUserMessage("Reply with the single word: ok")
                                    .build());
            return CHECK_MODEL;
        } catch (UnauthorizedException e) {
            throw new AiCheckException(
                    401, "Anthropic rejected the key. Check it and enter it again.", e);
        } catch (AnthropicException e) {
            throw new AiCheckException(
                    502, "Anthropic could not complete the check: " + e.getClass().getSimpleName(), e);
        }
    }
}
