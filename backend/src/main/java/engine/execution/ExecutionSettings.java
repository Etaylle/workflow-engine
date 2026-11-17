package engine.execution;

import java.nio.file.Path;
import java.time.Duration;
import java.math.BigDecimal;

/**
 * Configuration of the execution domain, all from the environment (see ExecutionConfig).
 *
 * @param workspaceRoot folder that holds one workspace per run, outside the repository
 * @param geminiCmd the Gemini CLI command (ENGINE_GEMINI_CMD)
 * @param geminiModel the model of the Gemini CLI builder (ENGINE_GEMINI_BUILD_MODEL)
 * @param codexCmd the Codex CLI command (ENGINE_CODEX_CMD)
 * @param codexModel the model of the Codex CLI builder (ENGINE_CODEX_BUILD_MODEL)
 * @param useSignIn start value of the Claude Code sign-in mode; the owner changes it at run time
 * @param createDatabase create a PostgreSQL database for the generated project of each run
 * @param maxResumes how many times a run interrupted by an engine restart may be resumed
 * @param limitWaitBase first wait when the rate limit or credit is exhausted; it doubles each retry
 * @param limitWaitMax longest single wait
 * @param limitMaxRetries waits before the run stops retrying by itself and waits for the client
 * @param fixAttempts how many times a failed step is sent back to the builder with the failure
 * @param previewLifetime how long a preview of the generated app runs once it answers
 * @param previewStartTimeout how long building and starting a preview may take before it fails
 * @param wholeCapMultiple factor on the per-step cap when the whole software is built as one unit
 */
record ExecutionSettings(Path workspaceRoot, String claudeBin, String model,
        String geminiCmd, String geminiModel, String codexCmd, String codexModel,
        boolean useSignIn, boolean createDatabase, Duration stepTimeout,
        Duration checkTimeout, int fixAttempts, int maxResumes,
        Duration limitWaitBase, Duration limitWaitMax, int limitMaxRetries, Duration previewLifetime,
        Duration previewStartTimeout, BigDecimal wholeCapMultiple) {
}
