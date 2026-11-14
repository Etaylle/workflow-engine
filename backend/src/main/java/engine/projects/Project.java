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
        String model,
        Instant createdAt,
        Instant updatedAt) {
}
