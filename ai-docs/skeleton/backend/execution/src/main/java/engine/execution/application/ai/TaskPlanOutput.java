package engine.execution.application.ai;

import engine.shared.StoryType;
import java.util.List;

/**
 * Structured output of the DEVELOPMENT SUPERVISOR at the start of a task (step "Plan the task").
 * One PlannedStep per step of the development workflow (settings.dev_workflow_steps, copied into the contract).
 * Maps to execution.step_runs: stepKey -> step_key, memberRoleKeys -> member_role_keys,
 * needed=false -> status NOT_NEEDED with reason as result. Steps whose story_types do not contain storyType are NOT_NEEDED.
 */
public record TaskPlanOutput(StoryType storyType, int budgetMinutes, List<PlannedStep> steps) {

    public record PlannedStep(String stepKey, boolean needed, String reason, List<String> memberRoleKeys,
            String instructions) {
    }
}
