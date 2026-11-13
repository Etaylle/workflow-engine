package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One AI call (no prompts, no keys).
 * Table: execution.agent_calls. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record AgentCall(
        UUID id,
        UUID projectId,
        UUID runId,
        String agent,
        String model,
        int inputTokens,
        int outputTokens,
        int durationMs,
        AgentCallStatus status,
        Instant createdAt) {
}
