package engine.execution;

import engine.shared.Harness;
import java.util.List;
import java.util.Map;

/**
 * The builders of the engine by name. In demo mode (ENGINE_AI_STUB without
 * ENGINE_EXECUTION_REAL_BUILDER) one stub does every build whatever the project chose; otherwise
 * the project's choice picks Claude Code or Gemini CLI.
 */
final class Builders {

    private final Map<Harness, RealBuilder> real;
    private final Builder demo;

    private Builders(Map<Harness, RealBuilder> real, Builder demo) {
        this.real = real;
        this.demo = demo;
    }

    /** The real builders: a project's choice picks one. */
    static Builders real(ClaudeCodeBuilder claude, GeminiCliBuilder gemini) {
        return new Builders(Map.of(Harness.CLAUDE_CODE, claude, Harness.GEMINI_CLI, gemini), null);
    }

    /** One builder for everything (the stub of the demo mode, a scripted builder of the tests). */
    static Builders single(Builder builder) {
        return new Builders(Map.of(), builder);
    }

    boolean demo() {
        return demo != null;
    }

    Builder forHarness(Harness harness) {
        return demo != null ? demo : real.get(harness);
    }

    /** What each builder needs, in the order Claude Code, Gemini CLI; everything ready in demo mode. */
    List<BuilderState> states() {
        return List.of(Harness.values()).stream().map(harness -> demo != null
                ? new BuilderState(harness.id(), harness.label(), true, true, "demo", null, true, null,
                        "stub", List.of())
                : real.get(harness).state()).toList();
    }
}
