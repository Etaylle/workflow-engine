package engine.ai;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** The user's Anthropic key for this engine session: memory only, never written anywhere. */
@Component
public class SessionKey {

    private final AtomicReference<String> key = new AtomicReference<>();

    public void set(String apiKey) {
        key.set(apiKey);
    }

    public void clear() {
        key.set(null);
    }

    public Optional<String> get() {
        return Optional.ofNullable(key.get());
    }
}
