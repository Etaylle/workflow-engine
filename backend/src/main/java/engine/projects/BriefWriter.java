package engine.projects;

import engine.shared.AiTarget;
/** Writes the brief of a wish; the real one calls Claude, tests use a stub. */
public interface BriefWriter {

    /**
     * Writes a brief in one planning call.
     *
     * @param target provider, key and model of this session
     * @param wish what the client wants built
     * @param correction why the previous answer was invalid, or null on the first try
     * @throws engine.shared.ApiProblem when the call fails (key, rate limit, time, service)
     * @throws engine.shared.InvalidAiOutputException when the answer cannot be read as a brief
     */
    Brief write(AiTarget target, String wish, String correction);
}
