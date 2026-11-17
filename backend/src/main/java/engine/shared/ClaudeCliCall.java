package engine.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.ObjectMappers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * One planning call through Claude Code with the sign-in of the engine machine (D-110): no API key.
 * The CLI answers once, without tools, in an empty folder, with the schema of the class as its
 * structured output.
 */
public final class ClaudeCliCall {

    private static final ObjectMapper JSON = ObjectMappers.jsonMapper();
    static final Duration TIMEOUT = Duration.ofMinutes(15);
    private static final int SHOWN = 400;

    private ClaudeCliCall() {
    }

    /**
     * Asks Claude Code one question and reads the answer as the given class.
     *
     * @param target the sign-in target: the command of the CLI and the model
     * @param system instructions of the call
     * @param user material of the call (wish, answers, correction)
     * @param type class the structured answer is read into
     * @throws ApiProblem when the CLI is missing, not signed in, limited, too slow or fails
     * @throws InvalidAiOutputException when the answer cannot be read as the class
     */
    public static <T> T structured(AiTarget target, String system, String user, Class<T> type) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("engine-planning");
            Path prompt = Files.writeString(dir.resolve("system.txt"), system, StandardCharsets.UTF_8);
            Path material = Files.writeString(dir.resolve("user.txt"), user, StandardCharsets.UTF_8);
            Path out = dir.resolve("answer.json");
            Path err = dir.resolve("answer.err");
            ProcessBuilder builder = new ProcessBuilder(command(target, prompt, schemaOf(type)))
                    .directory(dir.toFile()).redirectInput(material.toFile())
                    .redirectOutput(out.toFile()).redirectError(err.toFile());
            builder.environment().remove("ANTHROPIC_API_KEY");
            Process process = builder.start();
            if (!process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ApiProblem(504, "Claude Code did not answer in time", "Claude Code took longer"
                        + " than " + TIMEOUT.toMinutes() + " minutes for this planning step. Try again.");
            }
            return answer(process.exitValue(), read(out), read(err), type);
        } catch (IOException e) {
            throw new ApiProblem(409, "Claude Code not available", "The command \"" + target.baseUrl()
                    + "\" did not start on the engine machine (" + e.getMessage() + "). Install Claude"
                    + " Code and sign in, or enter an AI key in the key panel.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiProblem(503, "Planning interrupted", "The engine is stopping. Try again.", e);
        } finally {
            delete(dir);
        }
    }

    /** The command line: one answer, no tools, no session kept, nothing read from the folder. */
    static List<String> command(AiTarget target, Path systemPrompt, String schema) {
        return List.of(target.baseUrl(), "-p", "--system-prompt-file", systemPrompt.toString(), "--model",
                target.model(), "--output-format", "json", "--json-schema", schema, "--tools", "",
                "--strict-mcp-config", "--disable-slash-commands", "--no-session-persistence",
                "--setting-sources", "");
    }

    /**
     * The JSON Schema the Anthropic SDK derives for the class, as the API calls use it, without its
     * "$schema" line: the CLI refuses a schema that names a dialect it cannot load.
     */
    static String schemaOf(Class<?> type) {
        var format = AnthropicCall.params("s", "u", type).outputConfig().orElseThrow().format().orElseThrow();
        try {
            com.fasterxml.jackson.databind.node.ObjectNode schema =
                    JSON.valueToTree(format.schema()._additionalProperties());
            schema.remove("$schema");
            return JSON.writeValueAsString(schema);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("The schema of " + type.getSimpleName()
                    + " cannot be written as JSON.", e);
        }
    }

    /** Reads what the CLI printed: its structured output, or the problem it names. */
    static <T> T answer(int exitCode, String out, String err, Class<T> type) {
        JsonNode json;
        try {
            json = JSON.readTree(out);
        } catch (IOException e) {
            json = null;
        }
        if (json == null || !json.isObject()) {
            throw problem(shown(out.isBlank() ? err : out), exitCode);
        }
        String said = json.path("result").asText("");
        if (exitCode != 0 || json.path("is_error").asBoolean(false)
                || !"success".equals(json.path("subtype").asText(""))) {
            throw problem(shown(said.isBlank() ? json.path("subtype").asText("") + " " + err : said),
                    exitCode);
        }
        JsonNode structured = json.path("structured_output");
        if (!structured.isObject()) {
            throw new InvalidAiOutputException("Claude Code gave no structured answer.");
        }
        try {
            return JSON.treeToValue(structured, type);
        } catch (IOException | RuntimeException e) {
            throw new InvalidAiOutputException("The answer was not valid JSON for the schema.", e);
        }
    }

    /** The problem behind a failed call, by what Claude Code said. */
    private static ApiProblem problem(String said, int exitCode) {
        String lower = said.toLowerCase(Locale.ROOT);
        if (lower.contains("/login") || lower.contains("not logged in") || lower.contains("authenticat")
                || lower.contains("invalid api key")) {
            return new ApiProblem(409, "Claude Code is not signed in", "Claude Code on the engine machine"
                    + " is not signed in. Run `claude` once in a terminal there and log in, or enter an"
                    + " AI key in the key panel.");
        }
        if (lower.contains("limit") || lower.contains("quota") || lower.contains("credit")) {
            return new ApiProblem(429, "Claude usage limit reached", "Claude Code says: " + said
                    + " Try again when the limit is lifted, or enter an AI key in the key panel.");
        }
        return new ApiProblem(502, "Claude Code call failed", "Claude Code ended with exit code "
                + exitCode + (said.isBlank() ? " and said nothing." : ": " + said));
    }

    private static String shown(String text) {
        String line = text.strip().replace('\n', ' ');
        return line.length() > SHOWN ? line.substring(0, SHOWN) + "..." : line;
    }

    private static String read(Path file) throws IOException {
        return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
    }

    /** Removes the folder of the call; what it held is the material of one planning step. */
    private static void delete(Path dir) {
        if (dir == null) {
            return;
        }
        try (var files = Files.list(dir)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("The folder " + dir + " of a planning call could not"
                    + " be removed.", e);
        }
    }
}
