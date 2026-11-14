package engine.ai;

/** Offline check for tests and for ENGINE_AI_STUB=true: only the key "bad" is rejected. */
public class StubAiConnection implements AiConnection {

    @Override
    public String check(String apiKey) {
        if ("bad".equals(apiKey)) {
            throw new AiCheckException(401, "The stub rejected the key.", null);
        }
        return "stub";
    }
}
