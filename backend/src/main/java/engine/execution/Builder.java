package engine.execution;

import java.nio.file.Path;
import java.util.Map;

/** Does the work of one step in the workspace: Claude Code in production, a stub in tests. */
interface Builder {

    /** Fails with an ApiProblem when the builder cannot work (for example no key). */
    void checkReady();

    /** Runs one step; a failed step is an outcome with ok = false, not an exception. */
    StepOutcome run(StepRequest request);

    /** Runs the gate of a step (./check.sh in the workspace). */
    GateResult gate(Path workspace, Map<String, String> appEnv, Path logDir);
}
