package engine.execution;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;

/**
 * One invocation of the builder.
 *
 * @param workspace the folder the builder works in
 * @param stepKey key of the step (plan, build, ci, check)
 * @param prompt what the step must do
 * @param capUsd the most this invocation may cost
 * @param appEnv database settings of the generated project (never a secret of the user)
 * @param logDir where the raw output of the invocation is kept
 */
record StepRequest(Path workspace, String stepKey, String prompt, BigDecimal capUsd,
        Map<String, String> appEnv, Path logDir) {
}
