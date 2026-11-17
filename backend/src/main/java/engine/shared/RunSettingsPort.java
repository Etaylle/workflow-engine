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
     * @param review an independent review step: a separate reviewer builder runs it when the project
     *     has one; absent in the JSON of older runs and requests, which means false
     * @param reviewerOnly the step runs only when the project has a separate reviewer (the second
     *     opinion on the plan); a run without one leaves it out; absent means false
     */
    record Step(String key, String title, boolean gated, BigDecimal capUsd, String prompt,
            Boolean review, Boolean reviewerOnly) {

        /** A missing flag is false. */
        public Step {
            review = review != null && review;
            reviewerOnly = reviewerOnly != null && reviewerOnly;
        }
    }

    /**
     * The settings a run of the project starts with.
     *
     * @param profileName name of the step profile
     * @param steps the steps of one task in order
     * @param costLimitUsd the limit the client chose, or empty for the engine default
     */
    record RunSettings(String profileName, List<Step> steps, Optional<BigDecimal> costLimitUsd) {

        /** The steps a run takes: without a separate reviewer the reviewer-only steps are left out. */
        public List<Step> stepsFor(boolean hasReviewer) {
            return hasReviewer ? steps : steps.stream().filter(step -> !step.reviewerOnly()).toList();
        }
    }

    /** The profile and cost limit the project chose, or the defaults. */
    RunSettings forProject(UUID projectId);
}
