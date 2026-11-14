package engine.workflows;

import java.time.Instant;
import java.util.UUID;

/** A confirmed contract version; the frozen snapshot itself stays in the database. */
public record ContractView(UUID id, UUID projectId, int version, Instant confirmedAt) {
}
