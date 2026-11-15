package engine.workflows;

import engine.shared.AiTarget;
import engine.shared.InvalidAiOutputException;

/** Writes the business workflow from the wish, the answers and the requirements; the real one calls Claude, tests use a stub. */
public interface WorkflowWriter {

    /**
     * Writes a workflow draft in one planning call.
     *
     * @param target provider, key and model of this session
     * @param wish what the client wants built
     * @param material the brief, the answers of the client and the requirements as text
     * @param correction why the previous answer was invalid, or null on the first try
     * @throws engine.shared.ApiProblem when the call fails (key, rate limit, time, service)
     * @throws InvalidAiOutputException when the answer cannot be read as a workflow
     */
    WorkflowDraft write(AiTarget target, String wish, String material, String correction);
}
