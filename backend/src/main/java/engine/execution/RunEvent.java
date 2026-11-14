package engine.execution;

import java.time.Instant;

/** One numbered event of a run: a state change or a console line. */
public record RunEvent(long seq, String kind, String text, Instant at) {
}
