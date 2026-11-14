package engine.workflows;

/** Writes the business workflow from a wish; the real one calls Claude, tests use a stub. */
public interface WorkflowWriter {

    /**
     * Writes a workflow draft in one planning call.
     *
     * @param apiKey the user's key for this session
     * @param wish what the client wants built
     * @param correction why the previous answer was invalid, or null on the first try
     * @throws engine.shared.ApiProblem when the call fails (key, rate limit, time, service)
     * @throws InvalidDraftException when the answer cannot be read as a workflow
     */
    WorkflowDraft write(String apiKey, String wish, String correction);
}
