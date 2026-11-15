package engine.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.MessageCreateParams;
import engine.shared.AiTarget;
import engine.shared.AnthropicCall;
import engine.shared.ApiProblem;
import java.time.Duration;

/** Real check: one tiny message to Claude Haiku with a client made from the session key. */
public class AnthropicAiConnection implements AiConnection {

    static final String CHECK_MODEL = "claude-haiku-5-5";

    @Override
    public String check(AiTarget target) {
        AnthropicClient client =
                AnthropicOkHttpClient.builder()
                        .apiKey(target.apiKey())
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
        } catch (AnthropicException e) {
            ApiProblem problem = AnthropicCall.problem(e, target.apiKey());
            throw new AiCheckException(problem.status(), problem.getMessage(), e);
        }
    }
}
