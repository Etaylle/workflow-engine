package engine.execution;

import java.math.BigDecimal;

/**
 * What one builder invocation did: success, cost, turns, seconds, a one-line summary and the tokens
 * used (0 when the builder does not report them).
 */
record StepOutcome(boolean ok, BigDecimal costUsd, int turns, int seconds, String subtype,
        String summary, long tokens) {

    StepOutcome(boolean ok, BigDecimal costUsd, int turns, int seconds, String subtype, String summary) {
        this(ok, costUsd, turns, seconds, subtype, summary, 0);
    }

    /** The rate limit or the credit is exhausted: wait and retry, do not fix code. */
    boolean limited() {
        return !ok && LimitDetector.SUBTYPES.contains(subtype);
    }
}
