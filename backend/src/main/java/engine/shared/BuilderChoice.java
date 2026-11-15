package engine.shared;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The model and effort a project chose for its builder; null means the default of the engine (the
 * model) or of the CLI (the effort).
 *
 * @param model a Claude Code alias or a Gemini or Codex model id, or null
 * @param effort a Claude Code or Codex effort level, or null
 */
public record BuilderChoice(String model, String effort) {

    /** The Claude Code models the engine offers, cheapest first. */
    public static final List<String> CLAUDE_MODELS = List.of("haiku", "sonnet", "opus", "fable");

    /** The effort levels of Claude Code. */
    public static final List<String> EFFORTS = List.of("low", "medium", "high", "xhigh", "max");

    /** The reasoning-effort levels Codex CLI accepts for its models (D-102). */
    public static final List<String> CODEX_EFFORTS = List.of("low", "medium", "high", "xhigh");

    private static final int MAX_MODEL_LENGTH = 80;
    private static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._-]+");

    /**
     * Checks a choice for a builder. A blank value means the default.
     *
     * @throws ApiProblem 400 naming what is allowed
     */
    public static BuilderChoice checked(Harness harness, String model, String effort) {
        String m = blankToNull(model);
        String e = blankToNull(effort);
        if (harness == Harness.CODEX_CLI) {
            return codex(m, e);
        }
        if (harness == Harness.GEMINI_CLI) {
            if (e != null) {
                throw new ApiProblem(400, "No effort for Gemini CLI",
                        "Gemini CLI has no effort setting; send no builderEffort (or an empty one).");
            }
            if (m != null && (m.length() > MAX_MODEL_LENGTH || !MODEL_ID.matcher(m).matches())) {
                throw new ApiProblem(400, "Unknown model", "A Gemini model id has at most "
                        + MAX_MODEL_LENGTH + " characters of letters, digits, dot, underscore and dash.");
            }
            return new BuilderChoice(m, null);
        }
        if (m != null && !CLAUDE_MODELS.contains(m)) {
            throw new ApiProblem(400, "Unknown model", "For Claude Code the model must be one of "
                    + String.join(", ", CLAUDE_MODELS) + ", not \"" + m + "\".");
        }
        if (e != null && !EFFORTS.contains(e)) {
            throw new ApiProblem(400, "Unknown effort", "The effort must be one of "
                    + String.join(", ", EFFORTS) + ", not \"" + e + "\".");
        }
        return new BuilderChoice(m, e);
    }

    private static BuilderChoice codex(String m, String e) {
        if (m != null && (m.length() > MAX_MODEL_LENGTH || !MODEL_ID.matcher(m).matches())) {
            throw new ApiProblem(400, "Unknown model", "A Codex model id has at most "
                    + MAX_MODEL_LENGTH + " characters of letters, digits, dot, underscore and dash.");
        }
        if (e != null && !CODEX_EFFORTS.contains(e)) {
            throw new ApiProblem(400, "Unknown effort", "For Codex CLI the effort must be one of "
                    + String.join(", ", CODEX_EFFORTS) + ", not \"" + e + "\".");
        }
        return new BuilderChoice(m, e);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
