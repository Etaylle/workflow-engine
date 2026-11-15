package engine.execution;

import java.util.List;

/**
 * What one builder needs and whether it has it, as the API shows it.
 *
 * @param id "claude_code", "gemini_cli" or "codex_cli"
 * @param name name for people
 * @param ready nothing is missing: a run with this builder may start
 * @param installed the command runs on this machine
 * @param version what the command prints for --version, empty when not installed
 * @param signedIn "yes", "no" or "unknown" for Claude Code in machine sign-in mode and for Codex CLI; null otherwise
 * @param keySet the key this builder needs is set in the session (Anthropic key, Gemini key)
 * @param authMode how Claude Code authenticates ("anthropic_key", "machine_sign_in"); null for Gemini CLI
 * @param model the default model of the builder (a project that chose none uses it)
 * @param models the models to suggest, cheapest first
 * @param customModel any model id may be typed, not only the suggestions
 * @param efforts the effort levels the builder knows, empty when it has none
 * @param defaultEffort the effort used when none is chosen, null = the CLI's own default
 * @param missing what is missing, empty when ready
 */
record BuilderState(String id, String name, boolean ready, boolean installed, String version,
        String signedIn, boolean keySet, String authMode, String model, List<ModelOption> models, boolean customModel,
        List<String> efforts, String defaultEffort, List<Missing> missing) {

    /**
     * One model to suggest.
     *
     * @param id what the CLI takes after its model option
     * @param hint a short description for people
     */
    record ModelOption(String id, String hint) {
    }

    /**
     * One missing prerequisite.
     *
     * @param code stable name: claude_not_installed, no_anthropic_key, not_signed_in,
     *     gemini_not_installed, no_gemini_key, codex_not_installed, codex_not_signed_in
     * @param message what is missing and what to do, for people
     * @param instructionKey message key of the instruction text of the frontend, or null
     */
    record Missing(String code, String message, String instructionKey) {
    }
}
