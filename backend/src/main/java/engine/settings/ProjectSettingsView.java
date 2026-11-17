package engine.settings;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What a run of the project uses now: the chosen or the default profile, and the cost limit the
 * project chose (null when it chose none: the run then has no limit).
 */
record ProjectSettingsView(UUID stepProfileId, String stepProfileName, boolean usesDefaultProfile,
        BigDecimal costLimitUsd) {
}
