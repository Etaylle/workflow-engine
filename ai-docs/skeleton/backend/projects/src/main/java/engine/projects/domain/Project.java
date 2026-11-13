package engine.projects.domain;

import engine.shared.Provider;
import java.time.Instant;
import java.util.UUID;

/**
 * A client project (aggregate root).
 * Table: projects.projects. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Project(
        UUID id,
        String name,
        String wish,
        ProjectStatus status,
        AgentHarness agentHarness,
        Provider provider,
        String model,
        BackendStack backendStack,
        FrontendStack frontendStack,
        DocumentLanguage documentLanguage,
        int version,
        Instant createdAt,
        Instant updatedAt) {
}
