package engine.execution;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Stores run events and sends them to the open event streams: replay from a number, then live. */
@Component
class RunEvents {

    private static final long STREAM_TIMEOUT_MS = 10 * 60 * 1000L;
    private static final int REPLAY_LIMIT = 5000;

    /** One open stream; sends are serialized so replay and live events stay in order. */
    private static final class Subscriber {
        private final SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        private long last;

        synchronized boolean send(RunEvent event) {
            if (event.seq() <= last) {
                return true;
            }
            try {
                emitter.send(SseEmitter.event().id(String.valueOf(event.seq())).name(event.kind())
                        .data(event));
                last = event.seq();
                return true;
            } catch (IOException | IllegalStateException e) {
                return false;
            }
        }
    }

    private final RunStore store;
    private final Map<UUID, List<Subscriber>> streams = new ConcurrentHashMap<>();

    RunEvents(RunStore store) {
        this.store = store;
    }

    /** Stores the event and sends it to every open stream of the run. */
    RunEvent publish(UUID runId, String kind, String text) {
        RunEvent event = store.append(runId, kind, text);
        List<Subscriber> subscribers = streams.get(runId);
        if (subscribers != null) {
            subscribers.removeIf(s -> !s.send(event));
        }
        return event;
    }

    /** Opens a stream that first replays the events after the number, then follows the run. */
    SseEmitter subscribe(UUID runId, long after) {
        Subscriber subscriber = new Subscriber();
        subscriber.last = after;
        List<Subscriber> subscribers = streams.computeIfAbsent(runId, id -> new CopyOnWriteArrayList<>());
        Runnable remove = () -> subscribers.remove(subscriber);
        subscriber.emitter.onCompletion(remove);
        subscriber.emitter.onTimeout(remove);
        subscriber.emitter.onError(e -> remove.run());
        synchronized (subscriber) {
            subscribers.add(subscriber);
            for (RunEvent event : store.events(runId, after, REPLAY_LIMIT)) {
                if (!subscriber.send(event)) {
                    remove.run();
                    break;
                }
            }
        }
        return subscriber.emitter;
    }
}
