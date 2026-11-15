package engine.settings;

import java.math.BigDecimal;
import java.util.UUID;

/** What a run of the project uses now: the chosen or the default profile and limit. */
record ProjectSettingsView(UUID stepProfileId, String stepProfileName, boolean usesDefaultProfile,
        BigDecimal costLimitUsd, BigDecimal defaultCostLimitUsd, boolean usesDefaultLimit) {
}
