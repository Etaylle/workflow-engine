package engine.execution;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns one line of the streaming output of Claude Code ({@code --output-format stream-json}) or
 * Gemini CLI ({@code -o stream-json}) or Codex CLI ({@code exec --json}) into one short human line for the console, and keeps keys out.
 */
final class BuilderStream {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int MAX = 200;
    private static final Pattern KEY_SHAPE = Pattern.compile("sk-ant-[A-Za-z0-9_-]{8,}|sk-[A-Za-z0-9_-]{20,}"
            + "|AIza[A-Za-z0-9_-]{20,}|gh[pousr]_[A-Za-z0-9]{20,}");

    private BuilderStream() {
    }

    /** The human line for a Claude Code stream line, empty when the line says nothing worth showing. */
    static Optional<String> claude(String line, String workspace) {
        JsonNode json = parse(line);
        if (json == null) {
            return Optional.empty();
        }
        String type = json.path("type").asString("");
        if ("system".equals(type) && "init".equals(json.path("subtype").asString(""))) {
            return Optional.of("builder started (" + json.path("model").asString("model") + ")");
        }
        JsonNode content = json.path("message").path("content");
        if ("assistant".equals(type)) {
            for (JsonNode block : content) {
                if ("tool_use".equals(block.path("type").asString(""))) {
                    return Optional.of(tool(block.path("name").asString("tool"), block.path("input"),
                            workspace));
                }
                if ("text".equals(block.path("type").asString("")) && !block.path("text").asString("").isBlank()) {
                    return Optional.of("says: " + clip(block.path("text").asString("")));
                }
            }
        }
        if ("user".equals(type)) {
            for (JsonNode block : content) {
                if (block.path("is_error").asBoolean(false)) {
                    return Optional.of("tool error: " + clip(block.path("content").asString("")));
                }
            }
        }
        return Optional.empty();
    }

    /** The human line for a Gemini CLI stream line (event types init, message, tool_use, tool_result). */
    static Optional<String> gemini(String line, String workspace) {
        JsonNode json = parse(line);
        if (json == null) {
            return Optional.empty();
        }
        switch (json.path("type").asString("")) {
            case "init":
                return Optional.of("builder started (" + json.path("model").asString("model") + ")");
            case "message":
                String text = json.path("content").asString("");
                boolean chunk = json.path("delta").asBoolean(false);
                return "assistant".equals(json.path("role").asString("")) && !text.isBlank() && !chunk
                        ? Optional.of("says: " + clip(text)) : Optional.empty();
            case "tool_use":
                return Optional.of(tool(json.path("tool_name").asString("tool"), json.path("parameters"),
                        workspace));
            case "tool_result":
                return "error".equals(json.path("status").asString(""))
                        ? Optional.of("tool error: " + clip(json.path("error").path("message").asString("")))
                        : Optional.empty();
            default:
                return Optional.empty();
        }
    }

    /** The human line for a Codex CLI event (thread.started, item.started/completed, error, turn.failed). */
    static Optional<String> codex(String line, String workspace) {
        JsonNode json = parse(line);
        if (json == null) {
            return Optional.empty();
        }
        String type = json.path("type").asString("");
        JsonNode item = json.path("item");
        String itemType = item.path("type").asString("");
        switch (type) {
            case "thread.started":
                return Optional.of("builder started");
            case "error", "turn.failed":
                String message = json.path("error").path("message").asString(json.path("message").asString(""));
                return message.isBlank() ? Optional.empty() : Optional.of("error: " + clip(message));
            case "item.started":
                return "command_execution".equals(itemType)
                        ? Optional.of("runs " + clip(shellCommand(item.path("command").asString(""))))
                        : Optional.empty();
            case "item.completed":
                return codexCompleted(item, itemType, workspace);
            default:
                return Optional.empty();
        }
    }

    private static Optional<String> codexCompleted(JsonNode item, String itemType, String workspace) {
        switch (itemType) {
            case "agent_message":
                String text = item.path("text").asString("");
                return text.isBlank() ? Optional.empty() : Optional.of("says: " + clip(text));
            case "file_change":
                for (JsonNode change : item.path("changes")) {
                    String file = relative(change.path("path").asString(""), workspace);
                    String kind = change.path("kind").asString("");
                    return Optional.of(("add".equals(kind) ? "writes " : "delete".equals(kind) ? "deletes "
                            : "edits ") + file);
                }
                return Optional.empty();
            case "command_execution":
                int exit = item.path("exit_code").asInt(0);
                return exit == 0 || item.path("exit_code").isNull() ? Optional.empty()
                        : Optional.of("tool error: exit code " + exit + " from "
                                + clip(shellCommand(item.path("command").asString(""))));
            case "mcp_tool_call":
                return Optional.of("uses " + item.path("server").asString("mcp") + "."
                        + item.path("tool").asString("tool"));
            case "web_search":
                return Optional.of("searches " + clip(item.path("query").asString("")));
            default:
                return Optional.empty();
        }
    }

    /** The command a Codex shell item ran, without the shell wrapper ("bash -lc ls" becomes "ls"). */
    private static String shellCommand(String command) {
        String bare = command.replaceFirst("^(?:/\\S*/)?(?:ba|z)?sh -l?c\\s+", "");
        return bare.replaceAll("^(['\"])(.*)\\1$", "$2");
    }

    /**
     * What the stream of {@code codex exec --json} came to.
     *
     * @param completed a turn.completed event came
     * @param failure the message of the last error or turn.failed event, empty when none
     * @param tokens input plus output tokens of the turn
     * @param toolCalls completed commands, file changes and tool calls
     * @param message the last message of the agent
     */
    record CodexSummary(boolean completed, String failure, long tokens, int toolCalls, String message) {
    }

    /** Reads the whole stream of Codex CLI for its outcome. */
    static CodexSummary codexSummary(String stream) {
        boolean completed = false;
        String failure = "";
        long tokens = 0;
        int calls = 0;
        String message = "";
        for (String line : stream.split("\n")) {
            JsonNode json = parse(line);
            if (json == null) {
                continue;
            }
            switch (json.path("type").asString("")) {
                case "turn.completed":
                    completed = true;
                    tokens = json.path("usage").path("input_tokens").asLong(0)
                            + json.path("usage").path("output_tokens").asLong(0);
                    break;
                case "error", "turn.failed":
                    failure = json.path("error").path("message").asString(json.path("message").asString(failure));
                    break;
                case "item.completed":
                    String type = json.path("item").path("type").asString("");
                    calls += List.of("command_execution", "file_change", "mcp_tool_call").contains(type) ? 1 : 0;
                    message = "agent_message".equals(type) ? json.path("item").path("text").asString(message)
                            : message;
                    break;
                default:
                    break;
            }
        }
        return new CodexSummary(completed, failure, tokens, calls, message);
    }

    /** The line of the final result object of a stream, or null when the stream has none. */
    static JsonNode lastResult(String stream) {
        JsonNode result = null;
        for (String line : stream.split("\n")) {
            JsonNode json = line.startsWith("{") ? parse(line) : null;
            if (json != null && "result".equals(json.path("type").asString(""))) {
                result = json;
            }
        }
        return result;
    }

    /** The text the Gemini assistant wrote over the stream (its chunks joined). */
    static String assistantText(String stream) {
        StringBuilder text = new StringBuilder();
        for (String line : stream.split("\n")) {
            JsonNode json = line.startsWith("{") ? parse(line) : null;
            if (json != null && "message".equals(json.path("type").asString(""))
                    && "assistant".equals(json.path("role").asString(""))) {
                text.append(json.path("content").asString(""));
            }
        }
        return text.toString();
    }

    /** Replaces the given secrets and anything shaped like an API key or token by {@code [key]}. */
    static String redact(String text, Collection<String> secrets) {
        String clean = text;
        for (String secret : secrets) {
            if (secret != null && secret.length() >= 8) {
                clean = clean.replace(secret, "[key]");
            }
        }
        return KEY_SHAPE.matcher(clean).replaceAll("[key]");
    }

    private static String tool(String name, JsonNode input, String workspace) {
        String file = relative(first(input, "file_path", "path", "absolute_path", "dir_path"), workspace);
        switch (name) {
            case "Edit", "MultiEdit", "NotebookEdit", "replace", "edit_file":
                return "edits " + file;
            case "Write", "write_file":
                return "writes " + file;
            case "Read", "read_file", "read_many_files":
                return "reads " + file;
            case "Bash", "run_shell_command":
                return "runs " + clip(first(input, "command"));
            case "Glob", "Grep", "glob", "grep_search", "search_file_content":
                return "searches " + clip(first(input, "pattern"));
            case "list_directory":
                return "lists " + file;
            default:
                return "uses " + name + (file.isEmpty() ? "" : " " + file);
        }
    }

    private static String first(JsonNode input, String... names) {
        for (String name : names) {
            String value = input.path(name).asString("");
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String relative(String path, String workspace) {
        String prefix = workspace.endsWith("/") ? workspace : workspace + "/";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    private static String clip(String text) {
        String line = text.strip().replaceAll("\\s+", " ");
        return line.length() > MAX ? line.substring(0, MAX) + "..." : line;
    }

    private static JsonNode parse(String line) {
        if (!line.startsWith("{")) {
            return null;
        }
        try {
            return JSON.readTree(line);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
