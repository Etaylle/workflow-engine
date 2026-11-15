package engine.ai;

import engine.shared.AiTarget;

/** Offline check for tests and for ENGINE_AI_STUB=true: only the key "bad" is rejected. */
public class StubAiConnection implements AiConnection {

    @Override
    public String check(AiTarget target) {
        if ("bad".equals(target.apiKey())) {
            throw new AiCheckException(401, "The stub rejected the key.", null);
        }
        return "stub";
    }
}
