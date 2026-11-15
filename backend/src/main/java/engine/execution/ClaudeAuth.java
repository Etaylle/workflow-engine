package engine.execution;

import engine.shared.ApiProblem;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * How Claude Code authenticates when it builds: with the Anthropic key of the session, or with the
 * sign-in of this machine. Kept in engine memory with the keys; the environment variable
 * ENGINE_EXECUTION_USE_SIGN_IN only gives the value at start.
 */
@Component
class ClaudeAuth {

    /** The two ways; the id is the value of the API. */
    enum Mode {
        ANTHROPIC_KEY("anthropic_key"),
        MACHINE_SIGN_IN("machine_sign_in");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }
    }

    private volatile Mode mode;

    ClaudeAuth(ExecutionSettings settings) {
        this.mode = settings.useSignIn() ? Mode.MACHINE_SIGN_IN : Mode.ANTHROPIC_KEY;
    }

    Mode mode() {
        return mode;
    }

    boolean signIn() {
        return mode == Mode.MACHINE_SIGN_IN;
    }

    /** Chooses the mode by its API id, or answers 400 naming the two values. */
    void choose(String id) {
        String wanted = id == null ? "" : id.toLowerCase(Locale.ROOT);
        mode = Arrays.stream(Mode.values()).filter(m -> m.id().equals(wanted)).findFirst()
                .orElseThrow(() -> new ApiProblem(400, "Unknown sign-in mode",
                        "The mode must be \"anthropic_key\" or \"machine_sign_in\", not \"" + id + "\"."));
    }
}
