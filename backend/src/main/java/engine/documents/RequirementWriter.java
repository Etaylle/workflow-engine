package engine.documents;

import engine.shared.AiTarget;
/** Writes the requirements from the wish and the answers; the real one calls Claude. */
public interface RequirementWriter {

    /**
     * Writes the requirements in one planning call.
     *
     * @param target provider, key and model of this session
     * @param wish what the client wants built
     * @param material the brief and the answers of the client as text
     * @param correction why the previous answer was invalid, or null on the first try
     * @throws engine.shared.ApiProblem when the call fails (key, rate limit, time, service)
     * @throws engine.shared.InvalidAiOutputException when the answer cannot be used
     */
    RequirementDraft write(AiTarget target, String wish, String material, String correction);
}
