package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** The two builders read the same rules: GEMINI.md and CLAUDE.md of the template must not drift apart. */
class TemplateGuideTest {

    private static final Path TEMPLATES = Path.of("../templates");

    static Stream<Path> templates() throws IOException {
        try (Stream<Path> dirs = Files.list(TEMPLATES)) {
            return dirs.filter(dir -> Files.exists(dir.resolve("template.json"))
                    || Files.exists(dir.resolve("GEMINI.md"))).sorted().toList().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("templates")
    void geminiGuideHasTheSameContentAsTheClaudeGuide(Path template) throws IOException {
        String claude = Files.readString(template.resolve("CLAUDE.md"));
        String gemini = Files.readString(template.resolve("GEMINI.md"));

        assertThat(gemini).as("GEMINI.md differs from CLAUDE.md; copy one over the other").isEqualTo(claude);
    }
}
