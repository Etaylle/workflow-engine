package engine.workflows.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A test scenario (Given-When-Then) written by the AI from a criterion.
 * Table: workflows.scenarios. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record Scenario(
        UUID id,
        UUID nodeId,
        UUID criterionId,
        int seq,
        String gherkin,
        Instant createdAt,
        Instant updatedAt) {
}
