package engine.status;

import java.time.Instant;

/** What the page shows about the running engine; aiStub is true in demo mode (canned answers). */
public record EngineStatus(String name, String buildId, Instant serverTime, boolean aiStub) {
}
