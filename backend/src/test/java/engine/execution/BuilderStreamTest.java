package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BuilderStreamTest {

    private static List<String> lines(String name) throws IOException {
        return Files.readAllLines(Path.of("src/test/resources/streams/" + name), StandardCharsets.UTF_8);
    }

    @Test
    void claudeStreamBecomesShortHumanLines() throws IOException {
        List<String> shown = lines("claude.jsonl").stream()
                .map(line -> BuilderStream.claude(line, "/w/run-1")).flatMap(Optional::stream).toList();

        assertThat(shown).containsExactly("builder started (claude-sonnet-5-5)",
                "says: I will add the Duck class first.",
                "edits backend/src/main/java/engine/Duck.java", "runs ./check.sh",
                "tool error: tests failed: 1");
    }

    @Test
    void geminiStreamBecomesShortHumanLines() throws IOException {
        List<String> shown = lines("gemini.jsonl").stream()
                .map(line -> BuilderStream.gemini(line, "/w/run-1")).flatMap(Optional::stream).toList();

        assertThat(shown).containsExactly("builder started (gemini-3.1-flash-lite)",
                "writes src/Duck.java", "runs ./check.sh", "tool error: exit 1");
    }

    @Test
    void theResultLineAndTheAssistantTextAreFoundInTheStream() throws IOException {
        String claude = String.join("\n", lines("claude.jsonl"));
        String gemini = String.join("\n", lines("gemini.jsonl"));

        assertThat(BuilderStream.lastResult(claude).path("total_cost_usd").asDouble()).isEqualTo(0.42);
        assertThat(BuilderStream.lastResult(gemini).path("stats").path("total_tokens").asLong())
                .isEqualTo(900);
        assertThat(BuilderStream.assistantText(gemini)).isEqualTo("Built the duck.");
        assertThat(BuilderStream.lastResult("not json\n{broken")).isNull();
    }

    @Test
    void codexStreamBecomesShortHumanLinesAndAnOutcome() throws IOException {
        List<String> shown = lines("codex.jsonl").stream()
                .map(line -> BuilderStream.codex(line, "/w/run-1")).flatMap(Optional::stream).toList();

        assertThat(shown).containsExactly("builder started", "runs ./check.sh",
                "tool error: exit code 1 from ./check.sh", "writes src/duck.ts", "edits src/main.ts",
                "searches ascii duck", "says: Built the duck.");
        BuilderStream.CodexSummary summary = BuilderStream.codexSummary(String.join("\n", lines("codex.jsonl")));
        assertThat(summary.completed()).isTrue();
        assertThat(summary.failure()).isEmpty();
        assertThat(summary.tokens()).isEqualTo(24885);
        assertThat(summary.toolCalls()).isEqualTo(3);
        assertThat(summary.message()).isEqualTo("Built the duck.");
    }

    @Test
    void aFailedCodexTurnShowsTheErrorAndIsNotCompleted() {
        String stream = "{\"type\":\"error\",\"message\":\"boom\"}\n"
                + "{\"type\":\"turn.failed\",\"error\":{\"message\":\"boom\"}}";

        assertThat(BuilderStream.codex(stream.split("\n")[1], "/w")).contains("error: boom");
        BuilderStream.CodexSummary summary = BuilderStream.codexSummary(stream);
        assertThat(summary.completed()).isFalse();
        assertThat(summary.failure()).isEqualTo("boom");
    }

    @Test
    void garbageAndEmptyLinesShowNothing() {
        assertThat(BuilderStream.codex("{broken", "/w")).isEmpty();
        assertThat(BuilderStream.claude("plain text", "/w")).isEmpty();
        assertThat(BuilderStream.gemini("{broken", "/w")).isEmpty();
        assertThat(BuilderStream.claude("{\"type\":\"result\"}", "/w")).isEmpty();
    }

    @Test
    void keysAreRedactedByValueAndByShape() {
        String line = "runs curl -H key=SECRET-VALUE-123 sk-ant-api03-abcdefghijkl AIzaSyABCDEFGHIJKLMNOPQRSTUV";

        String clean = BuilderStream.redact(line, List.of("SECRET-VALUE-123"));

        assertThat(clean).doesNotContain("SECRET-VALUE-123", "sk-ant", "AIzaSy").contains("[key]");
    }
}
