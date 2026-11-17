package engine.execution;

import java.math.BigDecimal;

/**
 * What one builder invocation did: success, cost, turns, seconds, a one-line summary, the tokens
 * used (0 when the builder does not report them) and the session a next attempt can continue (null
 * when the builder has none).
 */
record StepOutcome(boolean ok, BigDecimal costUsd, int turns, int seconds, String subtype,
        String summary, long tokens, String session) {

    /** The step budget of the invocation was used up before the builder finished. */
    static final String BUDGET = "error_max_budget_usd";

    StepOutcome(boolean ok, BigDecimal costUsd, int turns, int seconds, String subtype, String summary) {
        this(ok, costUsd, turns, seconds, subtype, summary, 0, null);
    }

    StepOutcome(boolean ok, BigDecimal costUsd, int turns, int seconds, String subtype, String summary,
            long tokens) {
        this(ok, costUsd, turns, seconds, subtype, summary, tokens, null);
    }

    /** The same outcome with the session of the invocation. */
    StepOutcome inSession(String id) {
        return new StepOutcome(ok, costUsd, turns, seconds, subtype, summary, tokens, id);
    }

    /** The step budget ran out: more of the same attempt would not help, the client decides. */
    boolean overBudget() {
        return !ok && BUDGET.equals(subtype);
    }

    /** The rate limit or the credit is exhausted: wait and retry, do not fix code. */
    boolean limited() {
        return !ok && LimitDetector.SUBTYPES.contains(subtype);
    }
}
