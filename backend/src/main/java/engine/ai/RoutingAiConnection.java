package engine.ai;

import engine.shared.AiTarget;

/** The real check: Anthropic for an Anthropic key, the compatible endpoint for the other key. */
public class RoutingAiConnection implements AiConnection {

    private final AiConnection anthropic = new AnthropicAiConnection();
    private final AiConnection compatible = new OpenAiAiConnection();

    @Override
    public String check(AiTarget target) {
        return switch (target.provider()) {
            case ANTHROPIC -> anthropic.check(target);
            case OPENAI -> compatible.check(target);
        };
    }
}
