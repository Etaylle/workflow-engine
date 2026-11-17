package engine.shared;

/**
 * What one planning call talks to: the provider, the key of the session, and for an
 * OpenAI-compatible provider the base URL. The key never appears in toString. Without a key the
 * target can be Claude Code with the sign-in of the engine machine: then the key is null and the
 * base URL holds the command of the CLI.
 */
public record AiTarget(AiProvider provider, String apiKey, String baseUrl, String model) {

    /** Claude Code with the sign-in of the engine machine, started by the command. */
    public static AiTarget claudeSignIn(String command, String model) {
        return new AiTarget(AiProvider.ANTHROPIC, null, command, model);
    }

    /** True when the call goes through Claude Code and its sign-in instead of an API key. */
    public boolean signIn() {
        return provider == AiProvider.ANTHROPIC && apiKey == null;
    }

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
