package engine.shared;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What the domain "execution" may read of the domain "settings" when a run starts. */
public interface RunSettingsPort {

    /**
     * One development step: what Claude is told and how the orchestrator checks it.
     *
     * @param key short name, unique in the profile
     * @param title shown on the run screen
     * @param gated the orchestrator runs the check after the step and it must pass
     * @param capUsd most the step may cost in one invocation
     * @param prompt what Claude is told; {task} stands for the task title
     */
    record Step(String key, String title, boolean gated, BigDecimal capUsd, String prompt) {
    }

    /**
     * The settings a run of the project starts with.
     *
     * @param profileName name of the step profile
     * @param steps the steps of one task in order
     * @param costLimitUsd the limit the client chose, or empty for the engine default
     */
    record RunSettings(String profileName, List<Step> steps, Optional<BigDecimal> costLimitUsd) {
    }

    /** The profile and cost limit the project chose, or the defaults. */
    RunSettings forProject(UUID projectId);
}
