package engine.settings;

import engine.shared.RunSettingsPort.Step;
import java.util.List;
import java.util.UUID;

/** A step profile with its steps in order; seeded profiles are read-only. */
record StepProfileView(UUID id, String name, String description, boolean isDefault, boolean seeded,
        List<Step> steps) {
}
