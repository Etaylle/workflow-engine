package engine.execution.application.ai;

import java.util.List;

/**
 * Report of a TEAM MEMBER after its step (agents.md 15.2), returned by the coding agent in the sandbox.
 * Maps to execution.step_runs.result (short text) and execution.console_lines (one line per changed item).
 */
public record AgentReportOutput(String roleKey, List<String> changed, List<String> checked, List<String> notDone,
        List<String> openQuestions, String readingTaken) {
}
