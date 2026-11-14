package engine.execution;

import java.util.List;

/** Outcome of ./check.sh after a step: passed, and the last lines of its output. */
record GateResult(boolean passed, List<String> tail) {
}
