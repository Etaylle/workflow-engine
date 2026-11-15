package engine.ai;

import com.openai.errors.OpenAIException;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import engine.shared.AiTarget;
import engine.shared.ApiProblem;
import engine.shared.OpenAiCall;

/** Real check of an OpenAI-compatible provider: one tiny call with its base URL and model. */
public class OpenAiAiConnection implements AiConnection {

    @Override
    public String check(AiTarget target) {
        try {
            OpenAiCall.client(target)
                    .chat()
                    .completions()
                    .create(
                            ChatCompletionCreateParams.builder()
                                    .model(target.model())
                                    .addUserMessage("Reply with the single word: ok")
                                    .build());
            return target.model();
        } catch (OpenAIException e) {
            ApiProblem problem = OpenAiCall.problem(e, target);
            throw new AiCheckException(problem.status(), problem.getMessage(), e);
        }
    }
}
