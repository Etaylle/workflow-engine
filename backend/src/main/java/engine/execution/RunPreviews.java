package engine.execution;

import engine.shared.ApiProblem;
import engine.shared.Template;
import engine.shared.TemplateCatalog;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Starts the generated app of a run from a copy of its last green checkpoint, on a free local port
 * with its own database, so the client can try it while the builder goes on. One preview per run,
 * a few at a time, a bounded lifetime, a Stop, and cleanup of process, copy and database.
 */
@Component
class RunPreviews {

    private static final Logger LOG = LoggerFactory.getLogger(RunPreviews.class);
    private static final String SCRIPT = "start.sh";
    private static final int MAX_AT_ONCE = 2;
    private static final int MAX_APP_LINES = 3000;
    private static final long POLL_MS = 300;
    private static final Duration STOP_WAIT = Duration.ofSeconds(40);

    /** One preview; the thread of the session builds, watches and cleans up. */
    private static final class Session {
        final UUID runId;
        final Instant startedAt = Instant.now();
        volatile String status = "starting";
        volatile String message = "Building the app from the last green checkpoint";
        volatile Integer port;
        volatile Instant expiresAt;
        volatile String checkpoint;
        volatile boolean stopRequested;
        int linesSent;
        Thread thread;

        Session(UUID runId) {
            this.runId = runId;
        }

        PreviewView view() {
            return new PreviewView(status, port, startedAt, expiresAt, checkpoint, message);
        }

        boolean over() {
            return "stopped".equals(status) || "failed".equals(status);
        }
    }

    private final RunStore store;
    private final RunEvents events;
    private final ExecutionSettings settings;
    private final JdbcClient jdbc;
    private final TemplateCatalog templates;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    RunPreviews(RunStore store, RunEvents events, ExecutionSettings settings, JdbcClient jdbc,
            TemplateCatalog templates,
            @Value("${spring.datasource.url}") String dbUrl,
            @Value("${spring.datasource.username:}") String dbUser,
            @Value("${spring.datasource.password:}") String dbPassword) {
        this.store = store;
        this.events = events;
        this.settings = settings;
        this.jdbc = jdbc;
        this.templates = templates;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    PreviewView view(UUID runId) {
        Session session = sessions.get(runId);
        return session == null ? PreviewView.none() : session.view();
    }

    /** Starts the preview of the run; a preview that is already under way is returned as it is. */
    synchronized PreviewView start(UUID runId) {
        Session current = sessions.get(runId);
        if (current != null && !current.over()) {
            return current.view();
        }
        RunView run = store.view(runId).orElseThrow(
                () -> new ApiProblem(404, "Run not found", "No run " + runId));
        if (run.tasks().stream().noneMatch(task -> "done".equals(task.status()))) {
            throw new ApiProblem(409, "Nothing to try yet",
                    "A preview needs at least one finished task.");
        }
        if (sessions.values().stream().filter(s -> !s.over()).count() >= MAX_AT_ONCE) {
            throw new ApiProblem(409, "Too many previews", "Stop another preview first.");
        }
        Path workspace = Path.of(store.row(runId).orElseThrow().workspacePath());
        if (!Files.isDirectory(workspace.resolve(".git"))) {
            throw new ApiProblem(404, "Workspace gone", "The workspace of this run no longer exists.");
        }
        Session session = new Session(runId);
        sessions.put(runId, session);
        session.thread = Thread.ofPlatform().name("preview-" + runId).daemon().start(
                () -> work(session, new Workspace(workspace)));
        events.publish(runId, "preview", "starting");
        return session.view();
    }

    /** Stops the preview and waits until its process, copy and database are gone. */
    PreviewView stop(UUID runId) {
        Session session = sessions.get(runId);
        if (session == null) {
            return PreviewView.none();
        }
        session.stopRequested = true;
        join(session);
        return session.view();
    }

    /** The engine shuts down: no preview outlives it. */
    @PreDestroy
    void stopAll() {
        sessions.values().forEach(s -> s.stopRequested = true);
        sessions.values().forEach(RunPreviews::join);
    }

    private static void join(Session session) {
        try {
            session.thread.join(STOP_WAIT.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void work(Session session, Workspace workspace) {
        UUID runId = session.runId;
        Path dir = settings.workspaceRoot().resolve("previews").resolve(runId.toString());
        Path log = dir.resolveSibling(runId + ".log");
        String database = null;
        Process process = null;
        try {
            session.checkpoint = workspace.git("rev-parse", "HEAD").strip();
            Template template = templates.require(store.row(runId).orElseThrow().templateId());
            copyCheckpoint(workspace, template, session.checkpoint, dir);
            database = template.needsDatabase() ? createDatabase(runId) : null;
            Integer port = template.isConsole() ? null : freePort();
            session.port = port;
            process = launch(dir, log, port, database);
            watch(session, process, log, port);
        } catch (RuntimeException e) {
            LOG.warn("Preview of run {} failed", runId, e);
            session.message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            session.status = "failed";
        } finally {
            end(session, process, database, dir, log);
        }
    }

    private void end(Session session, Process process, String database, Path dir, Path log) {
        if (process != null) {
            kill(process);
        }
        if (database != null) {
            dropDatabase(database);
        }
        deleteTree(dir);
        deleteTree(log);
        if (!"failed".equals(session.status)) {
            session.status = "stopped";
        }
        session.port = null;
        session.expiresAt = null;
        events.publish(session.runId, "preview", session.status);
    }

    /** The loop of one preview: output to the console, the app answers, time is up, stop, exit. */
    private void watch(Session session, Process process, Path log, Integer port) {
        ProcessRunner.LineTail tail = new ProcessRunner.LineTail(log, line -> appLine(session, line));
        Instant startDeadline = Instant.now().plus(settings.previewStartTimeout());
        if (port == null) {
            session.expiresAt = Instant.now().plus(settings.previewLifetime());
            session.message = "The program runs";
            session.status = "running";
            events.publish(session.runId, "preview", "running");
        }
        try {
            while (!session.stopRequested) {
                tail.poll();
                if (!process.isAlive()) {
                    tail.poll();
                    exited(session, port == null, process.exitValue());
                    return;
                }
                if ("starting".equals(session.status)) {
                    checkAnswering(session, port, startDeadline);
                } else if (Instant.now().isAfter(session.expiresAt)) {
                    session.message = "The time of the preview is up";
                    return;
                }
                if ("failed".equals(session.status)) {
                    return;
                }
                Thread.sleep(POLL_MS);
            }
            session.message = "Stopped by the client";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            session.message = "Stopped";
        } finally {
            tail.poll();
            tail.finish();
        }
    }

    /** The process ended by itself: a console program that exits with 0 is simply finished. */
    private static void exited(Session session, boolean console, int exitCode) {
        if (console && exitCode == 0) {
            session.message = "The program ended with exit code 0";
            return;
        }
        session.message = (console ? "The program" : "The app") + " stopped with exit code " + exitCode;
        session.status = "failed";
    }

    private void checkAnswering(Session session, int port, Instant startDeadline) {
        if (answers(port)) {
            session.expiresAt = Instant.now().plus(settings.previewLifetime());
            session.message = "The app answers";
            session.status = "running";
            events.publish(session.runId, "preview", "running");
        } else if (Instant.now().isAfter(startDeadline)) {
            session.message = "The app did not answer within "
                    + settings.previewStartTimeout().toSeconds() + " seconds";
            session.status = "failed";
        }
    }

    private void appLine(Session session, String line) {
        int sent = session.linesSent++;
        if (sent < MAX_APP_LINES) {
            events.publish(session.runId, "app", BuilderStream.redact(line, List.of()));
        } else if (sent == MAX_APP_LINES) {
            events.publish(session.runId, "app", "(more output is not shown)");
        }
    }

    private void copyCheckpoint(Workspace workspace, Template template, String sha, Path dir) {
        try {
            Files.createDirectories(dir);
            Path archive = dir.resolveSibling(dir.getFileName() + ".tar");
            Files.write(archive, workspace.gitBytes("archive", "--format=tar", sha));
            extract(archive, dir);
            Files.deleteIfExists(archive);
            if (!Files.exists(dir.resolve(SCRIPT))) {
                Path fallback = template.dir().resolve(SCRIPT);
                if (!Files.isRegularFile(fallback)) {
                    throw new ApiProblem(409, "Preview not available", "This run has no " + SCRIPT
                            + " and the template of the engine has none to start the app with.");
                }
                Files.copy(fallback, dir.resolve(SCRIPT), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not copy the checkpoint " + sha, e);
        }
    }

    private static void extract(Path archive, Path dir) throws IOException {
        Path out = Files.createTempFile("engine-preview", ".out");
        try {
            ProcessRunner.Result result = ProcessRunner.run(
                    List.of("tar", "-xf", archive.toString(), "-C", dir.toString()), dir, Map.of(), out,
                    out, Duration.ofMinutes(2));
            if (result.exitCode() != 0) {
                throw new IllegalStateException("tar failed: " + Files.readString(out));
            }
        } finally {
            Files.deleteIfExists(out);
        }
    }

    private String createDatabase(UUID runId) {
        if (!settings.createDatabase()) {
            return null;
        }
        String name = "preview_" + runId.toString().replace("-", "").substring(0, 12);
        dropDatabase(name);
        jdbc.sql("create database " + name).update();
        return name;
    }

    private void dropDatabase(String name) {
        jdbc.sql("drop database if exists " + name + " with (force)").update();
    }

    private Process launch(Path dir, Path log, Integer port, String database) {
        ProcessBuilder builder = new ProcessBuilder("bash", SCRIPT).directory(dir.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile())
                .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
        Map<String, String> env = builder.environment();
        if (port != null) {
            env.put("APP_PORT", String.valueOf(port));
        }
        if (database != null) {
            env.put("APP_DB_URL", dbUrl.substring(0, dbUrl.lastIndexOf('/') + 1) + database);
            env.put("APP_DB_USER", dbUser);
            env.put("APP_DB_PASSWORD", dbPassword);
        }
        try {
            return builder.start();
        } catch (IOException e) {
            throw new IllegalStateException("Could not start " + SCRIPT + ": " + e.getMessage(), e);
        }
    }

    /** Asks the process tree to end, then forces it after a few seconds. */
    private static void kill(Process process) {
        List<ProcessHandle> tree = Stream.concat(process.descendants(), Stream.of(process.toHandle()))
                .toList();
        tree.forEach(ProcessHandle::destroy);
        try {
            process.waitFor(5, TimeUnit.SECONDS);
            if (tree.stream().anyMatch(ProcessHandle::isAlive)) {
                tree.forEach(ProcessHandle::destroyForcibly);
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tree.forEach(ProcessHandle::destroyForcibly);
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No free local port: " + e.getMessage(), e);
        }
    }

    private static boolean answers(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(path);
            }
        } catch (IOException e) {
            LOG.warn("Could not delete {}", root, e);
        }
    }
}
