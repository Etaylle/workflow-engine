package engine.execution;

import java.math.BigDecimal;

/** What one builder invocation did: success, cost, turns, seconds and a one-line summary. */
record StepOutcome(boolean ok, BigDecimal costUsd, int turns, int seconds, String subtype,
        String summary) {
}
