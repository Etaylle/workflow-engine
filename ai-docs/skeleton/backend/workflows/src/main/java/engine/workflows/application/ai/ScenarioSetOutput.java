package engine.workflows.application.ai;

import java.util.List;

/**
 * Test scenarios written by the WORKFLOW AGENT from the "done when" criteria of a task (page 5).
 * Each ScenarioDraft becomes a row of workflows.scenarios; criterionSeq links it to workflows.acceptance_criteria.
 * Validation: one scenario per criterion at least; each has Given, When and Then lines.
 */
public record ScenarioSetOutput(List<ScenarioDraft> scenarios) {

    public record ScenarioDraft(int criterionSeq, String gherkin) {
    }
}
