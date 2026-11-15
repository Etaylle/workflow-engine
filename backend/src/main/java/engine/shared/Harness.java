package engine.shared;

import java.util.Locale;

/** The builder of a project's software: Claude Code, Gemini CLI (D-77) or Codex CLI (D-102). */
public enum Harness {
    CLAUDE_CODE("claude_code", "Claude Code"),
    GEMINI_CLI("gemini_cli", "Gemini CLI"),
    CODEX_CLI("codex_cli", "Codex CLI");

    private final String id;
    private final String label;

    Harness(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** Value stored in the project and the run, and used in the API. */
    public String id() {
        return id;
    }

    /** Name for people, used in messages. */
    public String label() {
        return label;
    }

    /** The builder of an API id, or a 400 problem naming the allowed values. */
    public static Harness ofId(String id) {
        String wanted = id == null ? "" : id.toLowerCase(Locale.ROOT);
        for (Harness harness : values()) {
            if (harness.id.equals(wanted)) {
                return harness;
            }
        }
        throw new ApiProblem(400, "Unknown builder",
                "The builder must be \"claude_code\", \"gemini_cli\" or \"codex_cli\", not \"" + id + "\".");
    }
}
