package engine.questions;

import engine.shared.AiTarget;
/** Researches a wish and writes the questions; the real one calls Claude, tests use a stub. */
public interface QuestionWriter {

    /**
     * Writes the questions in one planning call.
     *
     * @param target provider, key and model of this session
     * @param wish what the client wants built
     * @param brief the brief of the wish as text
     * @param correction why the previous answer was invalid, or null on the first try
     * @throws engine.shared.ApiProblem when the call fails (key, rate limit, time, service)
     * @throws engine.shared.InvalidAiOutputException when the answer cannot be used
     */
    QuestionDraft write(AiTarget target, String wish, String brief, String correction);
}
