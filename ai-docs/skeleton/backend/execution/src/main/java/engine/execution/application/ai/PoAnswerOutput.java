package engine.execution.application.ai;

/**
 * Answer of the PRODUCT OWNER (answers from the contract) to a question of a team member.
 * fromContract=false means the agent chose the smallest reasonable option; then decision is required
 * and becomes a row of execution.decisions (and a line in DECISIONS.md).
 */
public record PoAnswerOutput(String answer, boolean fromContract, DecisionDraft decision) {

    public record DecisionDraft(String title, String reason) {
    }
}
