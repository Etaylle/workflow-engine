package engine.execution.application.ai;

import java.util.List;

/**
 * Structured output of the DEVELOPMENT SUPERVISOR at the end of a task (step "Sum up").
 * summary -> execution.task_runs.summary; decisions -> execution.decisions; openPoints -> console_lines (warn).
 */
public record TaskSummaryOutput(String summary, List<PoAnswerOutput.DecisionDraft> decisions, List<String> openPoints) {
}
