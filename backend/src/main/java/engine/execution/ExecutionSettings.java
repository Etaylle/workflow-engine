package engine.execution;

import java.nio.file.Path;
import java.time.Duration;
import java.math.BigDecimal;

/**
 * Configuration of the execution domain, all from the environment (see ExecutionConfig).
 *
 * @param workspaceRoot folder that holds one workspace per run, outside the repository
 * @param templateDir the project template that is copied into each workspace
 * @param useSignIn run Claude Code with the machine's own sign-in when no session key is set
 * @param createDatabase create a PostgreSQL database for the generated project of each run
 * @param costLimitUsd default cost limit of a run
 * @param maxResumes how many times a run interrupted by an engine restart may be resumed
 * @param fixAttempts how many times a failed step is sent back to the builder with the failure
 */
record ExecutionSettings(Path workspaceRoot, Path templateDir, String claudeBin, String model,
        boolean useSignIn, boolean createDatabase, BigDecimal costLimitUsd, Duration stepTimeout,
        Duration checkTimeout, int fixAttempts, int maxResumes) {
}
