package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The two builders read the same rules: GEMINI.md and CLAUDE.md of the template must not drift apart. */
class TemplateGuideTest {

    private static final Path TEMPLATE = Path.of("../templates/java-svelte-postgres");

    @Test
    void geminiGuideHasTheSameContentAsTheClaudeGuide() throws IOException {
        String claude = Files.readString(TEMPLATE.resolve("CLAUDE.md"));
        String gemini = Files.readString(TEMPLATE.resolve("GEMINI.md"));

        assertThat(gemini).as("GEMINI.md differs from CLAUDE.md; copy one over the other").isEqualTo(claude);
    }
}
