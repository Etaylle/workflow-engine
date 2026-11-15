package engine.ai;

import engine.shared.SessionKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operations getAiKey, setAiKey and deleteAiKey of api/openapi.yaml: the key of the session. */
@RestController
public class AiKeyController {

    private static final int SHOWN = 4;
    private static final int MIN_LENGTH_FOR_SUFFIX = 12;

    /** State of the key; never the key itself. */
    public record AiKeyState(boolean set, String last4) {
    }

    /** Request body; the key never appears in toString. */
    public record AiKeyRequest(@NotBlank String apiKey) {
        @Override
        public String toString() {
            return "AiKeyRequest[apiKey=***]";
        }
    }

    private final SessionKey sessionKey;

    public AiKeyController(SessionKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    @GetMapping("/api/ai/key")
    public AiKeyState state() {
        return sessionKey.get().map(AiKeyController::stateOf).orElse(new AiKeyState(false, null));
    }

    @PutMapping("/api/ai/key")
    public AiKeyState replace(@Valid @RequestBody AiKeyRequest request) {
        String key = request.apiKey().trim();
        sessionKey.set(key);
        return stateOf(key);
    }

    @DeleteMapping("/api/ai/key")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove() {
        sessionKey.clear();
    }

    /** Short keys show no suffix at all, so a short secret is never revealed. */
    static AiKeyState stateOf(String key) {
        String suffix = key.length() >= MIN_LENGTH_FOR_SUFFIX ? key.substring(key.length() - SHOWN) : "";
        return new AiKeyState(true, suffix);
    }
}
