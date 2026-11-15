package engine.shared;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The AI keys of this engine session: memory only, never written anywhere. One Anthropic key and
 * one key for an OpenAI-compatible provider (with its base URL and model).
 */
@Component
public class SessionKey {

    static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    static final String GEMINI_HOST = "generativelanguage.googleapis.com";
    static final String DEFAULT_MODEL = "gpt-5.4-mini";

    /** The OpenAI-compatible provider of the session. */
    public record Compatible(String apiKey, String baseUrl, String model) {
        @Override
        public String toString() {
            return "Compatible[baseUrl=" + baseUrl + ", model=" + model + ", key=***]";
        }
    }

    private final String defaultBaseUrl;
    private final String defaultModel;
    private volatile String anthropic;
    private volatile Compatible compatible;
    private volatile AiProvider preferred;

    @Autowired
    public SessionKey(
            @Value("${engine.openai.base-url:" + DEFAULT_BASE_URL + "}") String defaultBaseUrl,
            @Value("${engine.openai.model:" + DEFAULT_MODEL + "}") String defaultModel) {
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
    }

    public SessionKey() {
        this(DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String defaultModel() {
        return defaultModel;
    }

    public void setAnthropic(String apiKey) {
        anthropic = apiKey;
    }

    /** Sets the key of the compatible provider; a blank base URL or model is the default. */
    public void setCompatible(String apiKey, String baseUrl, String model) {
        String url = AiEndpoint.checked(baseUrl == null || baseUrl.isBlank() ? defaultBaseUrl : baseUrl);
        String chosen = model == null || model.isBlank() ? defaultModel : model.strip();
        compatible = new Compatible(apiKey, url, chosen);
    }

    public void clear(AiProvider provider) {
        if (provider == AiProvider.ANTHROPIC) {
            anthropic = null;
        } else {
            compatible = null;
        }
    }

    public void clearAll() {
        anthropic = null;
        compatible = null;
        preferred = null;
    }

    /** The Anthropic key: used by the planning calls and by the builder. */
    public Optional<String> anthropicKey() {
        return Optional.ofNullable(anthropic);
    }

    /** The key of the compatible slot when it points at Google Gemini: the Gemini CLI builder uses it. */
    public Optional<String> geminiKey() {
        return Optional.ofNullable(compatible)
                .filter(c -> c.baseUrl().contains(GEMINI_HOST)).map(Compatible::apiKey);
    }

    public Optional<Compatible> compatible() {
        return Optional.ofNullable(compatible);
    }

    /** Chooses which provider plans when both keys are set. */
    public void choose(AiProvider provider) {
        preferred = provider;
    }

    /** The provider the planning calls use: the only key set, or the chosen one when both are. */
    public Optional<AiProvider> active() {
        boolean hasAnthropic = anthropic != null;
        boolean hasCompatible = compatible != null;
        if (hasAnthropic && hasCompatible) {
            return Optional.of(preferred == null ? AiProvider.ANTHROPIC : preferred);
        }
        if (hasAnthropic) {
            return Optional.of(AiProvider.ANTHROPIC);
        }
        return hasCompatible ? Optional.of(AiProvider.OPENAI) : Optional.empty();
    }

    /** What the next planning call talks to, or empty when no key is set. */
    public Optional<AiTarget> planning() {
        String claude = anthropic;
        Compatible other = compatible;
        return active().map(provider -> provider == AiProvider.ANTHROPIC
                ? new AiTarget(provider, claude, null, AnthropicCall.MODEL)
                : new AiTarget(provider, other.apiKey(), other.baseUrl(), other.model()));
    }
}
