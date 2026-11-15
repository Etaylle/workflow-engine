package engine.shared;

import java.util.Locale;

/** The AI provider of the planning calls: Anthropic, or any OpenAI-compatible endpoint. */
public enum AiProvider {
    ANTHROPIC("anthropic"),
    OPENAI("openai");

    private final String id;

    AiProvider(String id) {
        this.id = id;
    }

    /** Value stored in the project and used in the API: "anthropic" or "openai". */
    public String id() {
        return id;
    }

    /** The provider of an API id, or a 400 problem naming the allowed values. */
    public static AiProvider ofId(String id) {
        String wanted = id == null ? "" : id.toLowerCase(Locale.ROOT);
        for (AiProvider provider : values()) {
            if (provider.id.equals(wanted)) {
                return provider;
            }
        }
        throw new ApiProblem(400, "Unknown AI provider",
                "The provider must be \"anthropic\" or \"openai\", not \"" + id + "\".");
    }
}
