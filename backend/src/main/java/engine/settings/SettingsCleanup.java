package engine.settings;

import engine.shared.ProjectCleanup;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Removes what a deleted project chose. */
@Component
class SettingsCleanup implements ProjectCleanup {

    private final SettingsStore store;

    SettingsCleanup(SettingsStore store) {
        this.store = store;
    }

    @Override
    public void deleteAllOf(UUID projectId) {
        store.deleteAllOf(projectId);
    }
}
