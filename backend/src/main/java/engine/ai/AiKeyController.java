package engine.ai;

import engine.shared.AiProvider;
import engine.shared.SessionKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operations getAiKeys, setAiKey, deleteAiKey and choosePlanningProvider of api/openapi.yaml: the
 * keys of the session, which are never shown (only the last 4 characters).
 */
@RestController
public class AiKeyController {

    private static final int SHOWN = 4;
    private static final int MIN_LENGTH_FOR_SUFFIX = 12;

    /** State of one key; never the key itself. */
    public record KeyState(boolean set, String last4) {
    }

    /** The compatible provider: key state plus the base URL and model in use. */
    public record CompatibleState(boolean set, String last4, String baseUrl, String model) {
    }

    /** What the planning calls use right now. */
    public record Planning(String provider, String model) {
    }

    /** Defaults from the environment, for the key panel. */
    public record Defaults(String baseUrl, String model) {
    }

    public record AiKeys(
            KeyState anthropic, CompatibleState openai, Planning planning, Defaults defaults) {
    }

    /** Request body; the key never appears in toString. */
    public record SetKeyRequest(@NotBlank String apiKey, String baseUrl, String model) {
        @Override
        public String toString() {
            return "SetKeyRequest[apiKey=***, baseUrl=" + baseUrl + ", model=" + model + "]";
        }
    }

    public record ChooseRequest(@NotBlank String provider) {
    }

    private final SessionKey sessionKey;

    public AiKeyController(SessionKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    @GetMapping("/api/ai/keys")
    public AiKeys state() {
        KeyState anthropic = sessionKey.anthropicKey().map(k -> new KeyState(true, suffix(k)))
                .orElse(new KeyState(false, null));
        CompatibleState openai = sessionKey.compatible()
                .map(c -> new CompatibleState(true, suffix(c.apiKey()), c.baseUrl(), c.model()))
                .orElse(new CompatibleState(false, null, null, null));
        Planning planning = sessionKey.planning()
                .map(t -> new Planning(t.provider().id(), t.model())).orElse(null);
        return new AiKeys(anthropic, openai, planning,
                new Defaults(sessionKey.defaultBaseUrl(), sessionKey.defaultModel()));
    }

    @PutMapping("/api/ai/keys/{provider}")
    public AiKeys replace(@PathVariable String provider, @Valid @RequestBody SetKeyRequest request) {
        String key = request.apiKey().trim();
        if (AiProvider.ofId(provider) == AiProvider.ANTHROPIC) {
            sessionKey.setAnthropic(key);
        } else {
            sessionKey.setCompatible(key, request.baseUrl(), request.model());
        }
        return state();
    }

    @DeleteMapping("/api/ai/keys/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String provider) {
        sessionKey.clear(AiProvider.ofId(provider));
    }

    @PutMapping("/api/ai/planning")
    public AiKeys choose(@Valid @RequestBody ChooseRequest request) {
        sessionKey.choose(AiProvider.ofId(request.provider()));
        return state();
    }

    /** Short keys show no suffix at all, so a short secret is never revealed. */
    static String suffix(String key) {
        return key.length() >= MIN_LENGTH_FOR_SUFFIX ? key.substring(key.length() - SHOWN) : "";
    }
}
