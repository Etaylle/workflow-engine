package engine.execution;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * One invocation of the builder.
 *
 * @param workspace the folder the builder works in
 * @param stepKey key of the step (plan, build, ci, check)
 * @param prompt what the step must do
 * @param capUsd the most this invocation may cost
 * @param appEnv database settings of the generated project (never a secret of the user)
 * @param logDir where the raw output of the invocation is kept
 * @param console receives short human lines about what the builder is doing, while it works
 * @param cancelled turns true when the client stops the run; the builder then ends the invocation
 */
record StepRequest(Path workspace, String stepKey, String prompt, BigDecimal capUsd,
        Map<String, String> appEnv, Path logDir, BooleanSupplier cancelled,
        Consumer<String> console) {
}
