package engine.shared;

/**
 * What one planning call talks to: the provider, the key of the session, and for an
 * OpenAI-compatible provider the base URL. The key never appears in toString.
 */
public record AiTarget(AiProvider provider, String apiKey, String baseUrl, String model) {

    @Override
    public String toString() {
        return "AiTarget[provider=" + provider + ", model=" + model + ", key=***]";
    }

    /** Name of the provider for messages: Anthropic, Gemini, OpenAI, Groq, OpenRouter or the host. */
    public String label() {
        if (provider == AiProvider.ANTHROPIC) {
            return "Anthropic";
        }
        String host = AiEndpoint.hostOf(baseUrl);
        if (host.contains("generativelanguage.googleapis.com")) {
            return "Gemini";
        }
        if (host.endsWith("openai.com")) {
            return "OpenAI";
        }
        if (host.endsWith("groq.com")) {
            return "Groq";
        }
        if (host.endsWith("openrouter.ai")) {
            return "OpenRouter";
        }
        return host.isEmpty() ? "OpenAI-compatible provider" : host;
    }
}
