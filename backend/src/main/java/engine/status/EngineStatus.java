package engine.status;

import java.time.Instant;

/** What the skeleton page shows about the running engine. */
public record EngineStatus(String name, String buildId, Instant serverTime) {
}
