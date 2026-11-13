package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The console of the AI work (insert-only).
 * Table: execution.console_lines. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record ConsoleLine(
        UUID id,
        UUID runId,
        UUID taskRunId,
        String source,
        ConsoleLevel level,
        String message,
        Instant createdAt) {
}
