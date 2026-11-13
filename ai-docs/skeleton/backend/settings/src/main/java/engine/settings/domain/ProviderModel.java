package engine.settings.domain;

import engine.shared.Provider;
import java.time.Instant;
import java.util.UUID;

/**
 * Which model is "best" and "standard" per provider.
 * Table: settings.provider_models. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record ProviderModel(
        UUID id,
        Provider provider,
        ModelTier tier,
        String modelId,
        Instant createdAt,
        Instant updatedAt) {
}
