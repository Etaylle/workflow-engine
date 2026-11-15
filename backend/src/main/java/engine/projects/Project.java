package engine.projects;

import java.time.Instant;
import java.util.UUID;

/** A client project as the API shows it. */
public record Project(
        UUID id,
        String name,
        String wish,
        String status,
        String agentHarness,
        String templateId,
        String buildMode,
        String model,
        String builderModel,
        String builderEffort,
        String reviewerHarness,
        String reviewerModel,
        String reviewerEffort,
        Instant createdAt,
        Instant updatedAt) {
}
