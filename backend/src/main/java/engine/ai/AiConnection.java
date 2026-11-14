package engine.ai;

/** The one call the AI check makes; the real one talks to Anthropic, tests use a stub. */
public interface AiConnection {

    /**
     * Makes one tiny call with the given key.
     *
     * @return the model that answered
     * @throws AiCheckException when the key is rejected or Anthropic cannot be reached
     */
    String check(String apiKey);
}
