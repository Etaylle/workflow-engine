package engine.execution;

import engine.shared.ApiProblem;
import engine.shared.Harness;
import java.util.List;
import java.util.stream.Collectors;

/** A builder that runs a command-line tool on this machine: it can say what it still needs. */
interface RealBuilder extends Builder {

    Harness harness();

    /** What is missing before this builder can work; empty when it is ready. */
    BuilderState state();

    @Override
    default void checkReady() {
        List<BuilderState.Missing> missing = state().missing();
        if (!missing.isEmpty()) {
            throw new ApiProblem(409, "Builder not ready: " + harness().label(),
                    harness().label() + " cannot build yet. Missing: "
                            + missing.stream().map(BuilderState.Missing::message)
                                    .collect(Collectors.joining(" ")));
        }
    }
}
