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
 */
record ExecutionSettings(Path workspaceRoot, Path templateDir, String claudeBin, String model,
        boolean useSignIn, boolean createDatabase, BigDecimal costLimitUsd, Duration stepTimeout,
        Duration checkTimeout) {
}
