package engine.execution;

import java.util.List;

/**
 * What one builder needs and whether it has it, as the API shows it.
 *
 * @param id "claude_code" or "gemini_cli"
 * @param name name for people
 * @param ready nothing is missing: a run with this builder may start
 * @param installed the command runs on this machine
 * @param version what the command prints for --version, empty when not installed
 * @param signedIn "yes", "no" or "unknown" for Claude Code in machine sign-in mode; null otherwise
 * @param keySet the key this builder needs is set in the session (Anthropic key, Gemini key)
 * @param authMode how Claude Code authenticates ("anthropic_key", "machine_sign_in"); null for Gemini CLI
 * @param model the model the builder uses
 * @param missing what is missing, empty when ready
 */
record BuilderState(String id, String name, boolean ready, boolean installed, String version,
        String signedIn, boolean keySet, String authMode, String model, List<Missing> missing) {

    /**
     * One missing prerequisite.
     *
     * @param code stable name: claude_not_installed, no_anthropic_key, not_signed_in,
     *     gemini_not_installed, no_gemini_key
     * @param message what is missing and what to do, for people
     * @param instructionKey message key of the instruction text of the frontend, or null
     */
    record Missing(String code, String message, String instructionKey) {
    }
}
