package engine.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks the installed command-line tools what they are: version (is it installed) and, for Claude
 * Code, whether the machine is signed in. Answers are cached for a short time because the
 * prerequisite check runs on every start of a run and on every read of the builder state.
 */
@Component
class ToolProbe {

    /** Whether the command runs and what it prints for --version. */
    record Installed(boolean installed, String version) {
    }

    private static final Duration CACHE = Duration.ofSeconds(30);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(20);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private record Cached(long at, Object value) {
    }

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    /** Runs {@code command --version}; not installed when it cannot start or exits with an error. */
    Installed installed(String command) {
        return cached("version:" + command, () -> {
            Optional<Output> out = run(List.of(command, "--version")).filter(o -> o.exit() == 0);
            return new Installed(out.isPresent(), out.map(o -> firstLine(o.text())).orElse(""));
        });
    }

    /**
     * Whether the Claude Code command is signed in on this machine, from its documented
     * {@code claude auth status} (JSON with "loggedIn"); empty when the answer cannot be read.
     */
    Optional<Boolean> claudeSignedIn(String command) {
        return cached("auth:" + command, () -> {
            Optional<Output> out = run(List.of(command, "auth", "status"));
            if (out.isEmpty()) {
                return Optional.<Boolean>empty();
            }
            try {
                JsonNode loggedIn = JSON.readTree(out.get().text()).path("loggedIn");
                return loggedIn.isBoolean() ? Optional.of(loggedIn.asBoolean()) : Optional.<Boolean>empty();
            } catch (RuntimeException e) {
                return Optional.<Boolean>empty();
            }
        });
    }

    /** Forgets the cached answers (after the owner changed something and wants a fresh look). */
    void forget() {
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, java.util.function.Supplier<T> probe) {
        long now = System.currentTimeMillis();
        Cached hit = cache.get(key);
        if (hit != null && now - hit.at() < CACHE.toMillis()) {
            return (T) hit.value();
        }
        T value = probe.get();
        cache.put(key, new Cached(now, value));
        return value;
    }

    /** Exit code and standard output; empty when the command cannot start or ran out of time. */
    private record Output(int exit, String text) {
    }

    private static Optional<Output> run(List<String> command) {
        Path out = null;
        Path err = null;
        try {
            out = Files.createTempFile("engine-probe", ".out");
            err = Files.createTempFile("engine-probe", ".err");
            ProcessRunner.Result result = ProcessRunner.run(command, Path.of("."), Map.of(), out, err,
                    PROBE_TIMEOUT);
            return result.timedOut() ? Optional.empty()
                    : Optional.of(new Output(result.exitCode(), Files.readString(out)));
        } catch (IllegalStateException | IOException e) {
            return Optional.empty();
        } finally {
            deleteQuietly(out);
            deleteQuietly(err);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                // a leftover temp file in the system temp folder is harmless
            }
        }
    }

    private static String firstLine(String text) {
        return text.lines().findFirst().orElse("").strip();
    }
}
