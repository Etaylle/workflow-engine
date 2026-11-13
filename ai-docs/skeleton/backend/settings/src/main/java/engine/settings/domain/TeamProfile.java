package engine.settings.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An editable team plus development workflow; the default comes from team.md and workflow.md.
 * Table: settings.team_profiles. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record TeamProfile(
        UUID id,
        String name,
        boolean isDefault,
        String source,
        TeamPreset preset,
        int version,
        Instant createdAt,
        Instant updatedAt) {
}
