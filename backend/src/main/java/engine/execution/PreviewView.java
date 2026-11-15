package engine.execution;

import java.time.Instant;

/**
 * The preview of a run as the API shows it.
 *
 * @param status none, starting, running, stopped or failed
 * @param port local port of the running app, null until it is chosen
 * @param startedAt when the preview was asked for
 * @param expiresAt when a running preview ends by itself, null before it answers
 * @param checkpoint the commit the preview was built from
 * @param message why it stopped or failed, or what it is doing
 */
public record PreviewView(String status, Integer port, Instant startedAt, Instant expiresAt,
        String checkpoint, String message) {

    static PreviewView none() {
        return new PreviewView("none", null, null, null, null, null);
    }
}
