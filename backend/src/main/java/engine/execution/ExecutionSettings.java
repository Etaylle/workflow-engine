package engine.execution;

import java.nio.file.Path;
import java.time.Duration;
import java.math.BigDecimal;

/**
 * Configuration of the execution domain, all from the environment (see ExecutionConfig).
 *
 * @param workspaceRoot folder that holds one workspace per run, outside the repository
 * @param templateDir the project template that is copied into each workspace
 * @param geminiCmd the Gemini CLI command (ENGINE_GEMINI_CMD)
 * @param geminiModel the model of the Gemini CLI builder (ENGINE_GEMINI_BUILD_MODEL)
 * @param useSignIn start value of the Claude Code sign-in mode; the owner changes it at run time
 * @param createDatabase create a PostgreSQL database for the generated project of each run
 * @param costLimitUsd default cost limit of a run
 * @param maxResumes how many times a run interrupted by an engine restart may be resumed
 * @param limitWaitBase first wait when the rate limit or credit is exhausted; it doubles each retry
 * @param limitWaitMax longest single wait
 * @param limitMaxRetries waits before the run stops retrying by itself and waits for the client
 * @param fixAttempts how many times a failed step is sent back to the builder with the failure
 */
record ExecutionSettings(Path workspaceRoot, Path templateDir, String claudeBin, String model,
        String geminiCmd, String geminiModel,
        boolean useSignIn, boolean createDatabase, BigDecimal costLimitUsd, Duration stepTimeout,
        Duration checkTimeout, int fixAttempts, int maxResumes,
        Duration limitWaitBase, Duration limitWaitMax, int limitMaxRetries) {
}
