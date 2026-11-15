package engine.shared;

import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.StructuredMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/** Builds the Messages API answer Claude would send, so tests run it through the SDK's parsing. */
public final class ClaudeAnswers {

    private static final ObjectMapper JSON = ObjectMappers.jsonMapper();

    private ClaudeAnswers() {
    }

    /** The class read from a finished answer whose text is the given JSON, via the real path. */
    public static <T> T read(Class<T> type, String json) {
        return ClaudeCall.answer(message(type, json, "end_turn"));
    }

    /** The structured message of an answer with the given text and stop reason. */
    public static <T> StructuredMessage<T> message(Class<T> type, String text, String stopReason) {
        try {
            String raw =
                    JSON.writeValueAsString(
                            Map.of(
                                    "id", "msg_test",
                                    "type", "message",
                                    "role", "assistant",
                                    "model", ClaudeCall.MODEL,
                                    "content", java.util.List.of(Map.of("type", "text", "text", text)),
                                    "stop_reason", stopReason,
                                    "stop_sequence", "",
                                    "usage", Map.of("input_tokens", 10, "output_tokens", 10)));
            return new StructuredMessage<>(type, JSON.readValue(raw, Message.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
