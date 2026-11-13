package engine.execution.application.ai;

import engine.execution.domain.AnswerOption;
import engine.execution.domain.ErrorClass;
import java.util.List;

/**
 * Advice of the DEVELOPMENT SUPERVISOR when a step fails (FR-AGT-10). The code first applies the automatic
 * reaction of the error class (settings.workflow_rules), then checks the action against the allowed list:
 *   TEMPORARY           -> retry the step, at most temporary_retries times (no AI needed)
 *   BROKEN_ENVIRONMENT  -> rebuild the environment once
 *   FLAKY_TEST          -> set the test aside, report it, continue
 *   REAL_DEFECT         -> FIX (new fix loop, owner = ownerRoleKey) up to max_fix_loops,
 *                          then RAISE_MODEL (one more try on the best model) if raise_model_before_asking
 *   REQUIREMENT_CONFLICT-> ASK_HUMAN
 * Writes execution.run_errors (error_class, attempts, auto_action), task_runs.fix_loops / model_raised,
 * execution.human_questions for ASK_HUMAN (expected, actual, requirement_refs, attempts_summary, cause, options).
 */
public record RecoveryOutput(ErrorClass errorClass, Action action, String ownerRoleKey, String hint,
        HumanQuestionDraft question) {

    public enum Action { RETRY, REBUILD_ENVIRONMENT, SET_ASIDE_TEST, FIX, RAISE_MODEL, SKIP_STEP, ASK_HUMAN, PAUSE_RUN }

    /** -> execution.human_questions (WFL-145 shape); 2..4 options, at most one recommended. */
    public record HumanQuestionDraft(String question, String expected, String actual, List<String> requirementRefs,
            String attemptsSummary, String cause, List<AnswerOption> options) {
    }
}
