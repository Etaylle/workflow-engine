package engine.shared;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * One planning call to the AI the user chose: Anthropic (Claude) or an OpenAI-compatible
 * provider by key, or Claude Code with the sign-in of the machine when no key is set.
 * Structured output by class, one validation with one retry.
 */
public final class PlanningCall {

    private PlanningCall() {
    }

    /**
     * Asks the AI one question and reads the answer as the given class.
     *
     * @param target provider, key, base URL and model of this session
     * @param system instructions of the call
     * @param user material of the call (wish, answers, correction)
     * @param type class the structured answer is read into
     * @throws ApiProblem when the call fails (key, rate limit, credit, time, service, refusal)
     * @throws InvalidAiOutputException when the answer cannot be read as the class
     */
    public static <T> T structured(AiTarget target, String system, String user, Class<T> type) {
        if (target.signIn()) {
            return ClaudeCliCall.structured(target, system, user, type);
        }
        return switch (target.provider()) {
            case ANTHROPIC -> AnthropicCall.structured(target.apiKey(), system, user, type);
            case OPENAI -> OpenAiCall.structured(target, system, user, type);
        };
    }

    /** The user message: the material, plus the reason of the last failure on the retry. */
    public static String withCorrection(String material, String correction) {
        return correction == null
                ? material
                : material + "\n\nYour previous answer was invalid: " + correction + "\nFix it.";
    }

    /**
     * Runs a call, validates the answer, and retries once with the reason when it is invalid.
     *
     * @param what name of the thing written, for the error message
     * @param call the call, given the reason of the first failure or null
     * @param validate throws {@link InvalidAiOutputException} with the reason when invalid
     */
    public static <T> T validated(String what, Function<String, T> call, Consumer<T> validate) {
        String correction = null;
        for (int attempt = 1; ; attempt++) {
            try {
                T result = call.apply(correction);
                validate.accept(result);
                return result;
            } catch (InvalidAiOutputException e) {
                if (attempt == 2) {
                    throw new ApiProblem(502, "Invalid " + what + " from the AI",
                            "The AI's " + what + " was invalid twice (" + e.getMessage()
                                    + "). Try again or reword the wish.", e);
                }
                correction = e.getMessage();
            }
        }
    }

    /** What the next planning call talks to, or a 409 problem telling the user to enter a key. */
    public static AiTarget requireTarget(SessionKey sessionKey) {
        return sessionKey.planning().orElseThrow(() -> new ApiProblem(409, "AI key missing",
                "No AI key is set, and planning with the Claude Code sign-in of this machine is"
                        + " switched off (ENGINE_PLANNING_SIGN_IN). Enter an Anthropic key or a key of an"
                        + " OpenAI-compatible provider (for example Gemini) in the key panel."));
    }
}
