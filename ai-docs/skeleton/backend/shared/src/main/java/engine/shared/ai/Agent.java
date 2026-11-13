package engine.shared.ai;

import java.util.UUID;

/**
 * Every engine agent (layer B). Implementations live in the adapters of their domain and are called only by
 * the orchestrator through the AiTaskRunner (schema validation, one retry, tool-call limit, agent_calls row).
 */
public interface Agent<I, O> {

    String name();

    O run(I input, AgentContext context);

    /** Model, key handle, budget and ids of the current call. No key value is ever stored here. */
    record AgentContext(String provider, String model, String keyHandle, UUID projectId, UUID runId, int maxToolCalls) {
    }
}
