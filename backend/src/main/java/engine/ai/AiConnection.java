package engine.ai;

import engine.shared.AiTarget;

/** The one call the AI check makes; the real one talks to the provider, tests use a stub. */
public interface AiConnection {

    /**
     * Makes one tiny call with the given key (and base URL and model).
     *
     * @return the model that answered
     * @throws AiCheckException when the key is rejected or the provider cannot be reached
     */
    String check(AiTarget target);
}
