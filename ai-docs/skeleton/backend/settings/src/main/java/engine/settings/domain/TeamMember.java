package engine.settings.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One member of the AI team (editable).
 * Table: settings.team_members. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record TeamMember(
        UUID id,
        UUID profileId,
        String roleKey,
        String name,
        String mission,
        String instructions,
        ModelTier modelTier,
        boolean active,
        boolean canDisable,
        List<String> writePaths,
        int seq,
        Instant createdAt,
        Instant updatedAt) {
}
