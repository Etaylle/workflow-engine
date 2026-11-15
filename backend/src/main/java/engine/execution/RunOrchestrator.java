package engine.execution;

import engine.shared.ProjectPort;
import engine.shared.RunSettingsPort.Step;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The state machine of a run, in code: preparing, executing, then done, or paused when a step
 * fails. Tasks are built one after the other, each by the steps of the profile, one invocation
 * of the builder per step and a git commit after every green step.
 */
@Component
class RunOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(RunOrchestrator.class);
    static final BigDecimal MIN_BUDGET = new BigDecimal("0.30");

    private final RunStore store;
    private final RunEvents events;
    private final Builders builders;
    private final ExecutionSettings settings;
    private final ProjectPort projects;
    private final JdbcClient jdbc;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final Set<UUID> stopRequested = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "run-orchestrator");
        thread.setDaemon(true);
        return thread;
    });

    RunOrchestrator(RunStore store, RunEvents events, Builders builders, ExecutionSettings settings,
            ProjectPort projects, JdbcClient jdbc, @Value("${spring.datasource.url}") String dbUrl,
            @Value("${spring.datasource.username:}") String dbUser,
            @Value("${spring.datasource.password:}") String dbPassword) {
        this.store = store;
        this.events = events;
        this.builders = builders;
        this.settings = settings;
        this.projects = projects;
        this.jdbc = jdbc;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    static String databaseName(UUID runId) {
        return "run_" + runId.toString().replace("-", "").substring(0, 12);
    }

    /** Starts (or continues) the run in the background and returns at once. */
    void submit(UUID runId, String contract) {
        active.add(runId);
        executor.submit(() -> {
            try {
                if (!stopRequested.contains(runId)) {
                    execute(runId, contract);
                }
            } catch (RuntimeException e) {
                LOG.error("Run {} stopped on an unexpected error", runId, e);
                if (!stopRequested.contains(runId)) {
                    interrupt(runId, "The run could not continue: " + e.getMessage());
                }
            } finally {
                finish(runId);
            }
        });
    }

    /** The thread of a run ends: after a stop the workspace goes back to the last green checkpoint. */
    private void finish(UUID runId) {
        try {
            if (stopRequested.contains(runId)) {
                resetWorkspace(runId);
            }
        } finally {
            stopRequested.remove(runId);
            active.remove(runId);
        }
    }

    private void resetWorkspace(UUID runId) {
        store.row(runId).map(row -> Path.of(row.workspacePath())).filter(Files::isDirectory)
                .filter(dir -> Files.isDirectory(dir.resolve(".git")))
                .ifPresent(dir -> new Workspace(dir).resetToCheckpoint());
    }

    /** True while a thread builds this run (the workspace is then not safe to read or zip). */
    boolean isBuilding(UUID runId) {
        return active.contains(runId);
    }

    /**
     * The client stops the run: a build under way ends at once and its thread resets the workspace
     * to the last checkpoint; a run that only waits is reset here.
     */
    void stop(UUID runId) {
        if (active.contains(runId)) {
            stopRequested.add(runId);
        } else {
            resetWorkspace(runId);
        }
    }

    private boolean stopped(UUID runId) {
        return stopRequested.contains(runId);
    }

    @jakarta.annotation.PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private void execute(UUID runId, String contract) {
        RunStore.RunRow run = store.row(runId).orElseThrow();
        Workspace workspace = new Workspace(Path.of(run.workspacePath()));
        Path logDir = logDir(workspace);
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create " + logDir, e);
        }
        Map<String, String> appEnv;
        if ("preparing".equals(run.status())) {
            events.publish(runId, "console", "Preparing the workspace from the template");
            workspace.prepare(settings.templateDir(), contract);
            createDatabase(run);
            appEnv = appEnv(run);
            store.setRun(runId, "executing", null);
            events.publish(runId, "run", "executing");
        } else {
            workspace.resetToCheckpoint();
            store.resetUnfinished(runId);
            appEnv = appEnv(run);
            events.publish(runId, "run", "executing: continued from the last checkpoint");
        }
        Ctx ctx = new Ctx(run, workspace, logDir, appEnv);
        List<RunStore.TaskRow> tasks = store.tasksToBuild(runId);
        for (int i = 0; i < tasks.size(); i++) {
            if (!"done".equals(tasks.get(i).status()) && !buildTask(ctx, contract, tasks, i)) {
                return;
            }
        }
        store.setRun(runId, "done", null);
        projects.markStatus(run.projectId(), "done");
        events.publish(runId, "run", "done");
    }

    static Path logDir(Workspace workspace) {
        return workspace.dir().resolveSibling(workspace.dir().getFileName() + "-logs");
    }

    private void createDatabase(RunStore.RunRow run) {
        if (settings.createDatabase()) {
            jdbc.sql("create database " + run.databaseName()).update();
            events.publish(run.id(), "console", "Database " + run.databaseName() + " created for the project");
        }
    }

    private Map<String, String> appEnv(RunStore.RunRow run) {
        if (!settings.createDatabase()) {
            return Map.of();
        }
        Map<String, String> env = new HashMap<>();
        env.put("APP_DB_URL", dbUrl.substring(0, dbUrl.lastIndexOf('/') + 1) + run.databaseName());
        env.put("APP_DB_USER", dbUser);
        env.put("APP_DB_PASSWORD", dbPassword);
        env.put("APP_PORT", "18080");
        return env;
    }

    /** What one run needs while it builds: the row, the workspace, the log folder, the app settings. */
    private record Ctx(RunStore.RunRow run, Workspace workspace, Path logDir, Map<String, String> appEnv) {
    }

    /** The result of one attempt at a step: passed with a summary, or failed with what went wrong. */
    private record Attempt(boolean passed, boolean limited, String text) {
    }

    private boolean buildTask(Ctx ctx, String contract, List<RunStore.TaskRow> tasks, int index) {
        RunStore.TaskRow task = tasks.get(index);
        store.setTask(task.id(), "running");
        events.publish(ctx.run().id(), "task", "Task " + (index + 1) + " of " + tasks.size() + ": " + task.title());
        List<String> done = tasks.subList(0, index).stream().map(RunStore.TaskRow::title).toList();
        List<String> later = tasks.subList(index + 1, tasks.size()).stream().map(RunStore.TaskRow::title).toList();
        ctx.workspace().writeTask(contract, task.title(), task.description(), done, later);
        for (int s = 0; s < task.steps().size(); s++) {
            if (!"done".equals(task.steps().get(s).status()) && !buildStep(ctx, task, s, index + 1)) {
                return false;
            }
        }
        archiveDocs(ctx.workspace().dir(), index + 1);
        ctx.workspace().commit("Task " + (index + 1) + " done: " + task.title());
        store.setTask(task.id(), "done");
        events.publish(ctx.run().id(), "task", "Task " + (index + 1) + " done: " + task.title());
        return true;
    }

    /** Runs a step; a failed attempt goes back to the builder with the failure, a bounded number of times. */
    private boolean buildStep(Ctx ctx, RunStore.TaskRow task, int stepIndex, int taskNo) {
        UUID runId = ctx.run().id();
        RunStore.StepRow row = task.steps().get(stepIndex);
        Step def = withTask(ctx.run().steps().get(stepIndex), task.title());
        store.setStep(row.id(), "running", null);
        events.publish(runId, "step", "Step " + def.title() + " started");
        int attempts = 1 + settings.fixAttempts();
        String failure = null;
        int waits = 0;
        int attempt = 1;
        while (attempt <= attempts) {
            if (stopped(runId)) {
                return false;
            }
            BigDecimal limit = store.costLimitOf(runId);
            BigDecimal remaining = limit.subtract(store.costOf(runId));
            if (remaining.compareTo(MIN_BUDGET) < 0) {
                return costLimit(ctx.run().id(), limit, task, row);
            }
            announceFix(runId, def, attempt, waits);
            Attempt result = attempt(ctx, row, def, remaining, failure);
            if (stopped(runId)) {
                return false;
            }
            if (result.passed()) {
                return completeStep(ctx, row, def, taskNo, result.text());
            }
            if (result.limited()) {
                if (!waitForTokens(ctx, task, row, result.text(), ++waits)) {
                    return false;
                }
                continue;
            }
            waits = 0;
            failure = result.text();
            events.publish(runId, "console", "Attempt " + attempt + " failed: " + failure);
            attempt++;
        }
        return askClient(ctx.run(), task, row, def.title(), attempts, failure);
    }

    /** The prompt of a step may name the task as {task}. */
    private static Step withTask(Step step, String taskTitle) {
        return new Step(step.key(), step.title(), step.gated(), step.capUsd(),
                step.prompt().replace("{task}", taskTitle));
    }

    private void announceFix(UUID runId, Step def, int attempt, int waits) {
        if (attempt > 1 && waits == 0) {
            events.publish(runId, "console", "Fix attempt " + (attempt - 1) + " of "
                    + settings.fixAttempts() + " for " + def.title());
        }
    }

    /** A green step: commit the workspace (the checkpoint), record it, mark the step done. */
    private boolean completeStep(Ctx ctx, RunStore.StepRow row, Step def, int taskNo, String summary) {
        String sha = ctx.workspace().commit("Task " + taskNo + " step " + def.key() + " green");
        store.addCheckpoint(row.id(), sha);
        store.setStep(row.id(), "done", summary);
        events.publish(ctx.run().id(), "step", "Step " + def.title() + " done");
        return true;
    }

    /**
     * The rate limit or the credit is exhausted: the run waits (status waiting, kind tokens) with a
     * growing, bounded delay and then tries the same attempt again. After the allowed number of
     * waits it stops retrying by itself and waits for the client to continue it.
     *
     * @return true when the step should be tried again, false when the run stays waiting or stopped
     */
    private boolean waitForTokens(Ctx ctx, RunStore.TaskRow task, RunStore.StepRow row, String what,
            int retry) {
        UUID runId = ctx.run().id();
        if (retry > settings.limitMaxRetries()) {
            store.setStep(row.id(), "waiting", null);
            store.setTask(task.id(), "pending");
            String reason = "Waiting for tokens: " + what + ". Still limited after "
                    + settings.limitMaxRetries() + " tries; continue the run when the limit is lifted.";
            store.setWaiting(runId, "tokens", reason);
            events.publish(runId, "run", "waiting: " + reason);
            return false;
        }
        long delayMs = Math.min(settings.limitWaitMax().toMillis(),
                settings.limitWaitBase().toMillis() << Math.min(retry - 1, 20));
        String reason = "Waiting for tokens: " + what + ". Try " + retry + " of "
                + settings.limitMaxRetries() + " in " + Math.max(1, delayMs / 1000) + " s.";
        store.setWaiting(runId, "tokens", reason);
        events.publish(runId, "run", "waiting: " + reason);
        if (!sleepUnlessStopped(runId, delayMs)) {
            return false;
        }
        store.setRun(runId, "executing", null);
        events.publish(runId, "run", "executing: trying again");
        return true;
    }

    /** Sleeps in short slices so a stop is noticed; false when the run was stopped meanwhile. */
    private boolean sleepUnlessStopped(UUID runId, long millis) {
        long end = System.nanoTime() + millis * 1_000_000L;
        while (System.nanoTime() < end) {
            if (stopped(runId)) {
                return false;
            }
            try {
                Thread.sleep(Math.min(50, Math.max(1, (end - System.nanoTime()) / 1_000_000L)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !stopped(runId);
    }

    private String hintsOf(RunStore.StepRow row, Step def) {
        List<String> hints = store.hintsOf(row.id());
        return hints.isEmpty() ? def.prompt() : def.prompt() + "\n\nThe client says about this step: "
                + String.join(" ", hints);
    }

    private Attempt attempt(Ctx ctx, RunStore.StepRow row, Step def, BigDecimal remaining,
            String previousFailure) {
        UUID runId = ctx.run().id();
        String base = hintsOf(row, def);
        String prompt = previousFailure == null ? base : base
                + "\n\nYour previous attempt at this step failed. What went wrong:\n" + previousFailure
                + "\n\nFix this. Change only what is needed, then run ./check.sh until it prints"
                + " CHECK PASSED.";
        Builder builder = builders.forHarness(ctx.run().harness());
        StepOutcome outcome = builder.run(new StepRequest(ctx.workspace().dir(), def.key(), prompt,
                def.capUsd().min(remaining), ctx.appEnv(), ctx.logDir(), () -> stopped(runId)));
        UUID call = store.addAgentCall(row.id(), outcome);
        events.publish(runId, "console", def.title() + ": " + outcome.summary() + " (USD "
                + outcome.costUsd().setScale(2, java.math.RoundingMode.HALF_UP) + ", "
                + outcome.turns() + " turns)");
        if (!outcome.ok()) {
            return new Attempt(false, outcome.limited(), def.title() + " failed: " + outcome.summary());
        }
        if (def.gated()) {
            GateResult gate = builder.gate(ctx.workspace().dir(), ctx.appEnv(), ctx.logDir());
            gate.tail().forEach(line -> events.publish(runId, "console", "check: " + line));
            if (!gate.passed()) {
                String text = "The check after " + def.title() + " did not pass:\n"
                        + String.join("\n", gate.tail());
                store.markCheckFailed(call, text);
                return new Attempt(false, false, text);
            }
        }
        return new Attempt(true, false, outcome.summary());
    }

    /** Not fixable after the attempts: the run waits with one question for the client. */
    private boolean askClient(RunStore.RunRow run, RunStore.TaskRow task, RunStore.StepRow step,
            String stepTitle, int attempts, String failure) {
        store.setStep(step.id(), "failed", failure);
        store.setTask(task.id(), "failed");
        String question = stepTitle + " could not be fixed after " + attempts + " attempts. "
                + "What should the engine do? Last failure: " + failure;
        store.addQuestion(run.id(), step.id(), question);
        store.setWaiting(run.id(), "question", question);
        events.publish(run.id(), "run", "waiting: question for the client");
        return false;
    }

    /** The cost limit is reached: the run waits until the client raises it. */
    private boolean costLimit(UUID runId, BigDecimal limit, RunStore.TaskRow task, RunStore.StepRow step) {
        store.setStep(step.id(), "waiting", null);
        store.setTask(task.id(), "pending");
        String reason = "The cost limit of USD " + limit + " is reached. Raise the limit to continue.";
        store.setWaiting(runId, "cost_limit", reason);
        events.publish(runId, "run", "waiting: " + reason);
        return false;
    }

    private void interrupt(UUID runId, String reason) {
        store.setRun(runId, "interrupted", reason);
        events.publish(runId, "run", "interrupted: " + reason);
    }

    /** Moves the documents of a finished task into docs/task-N so the next task starts clean. */
    private static void archiveDocs(Path dir, int taskNo) {
        Path docs = dir.resolve("docs");
        Path target = docs.resolve("task-" + taskNo);
        if (!Files.isDirectory(docs)) {
            return;
        }
        try (Stream<Path> files = Files.list(docs)) {
            List<Path> toMove = new ArrayList<>(files.filter(Files::isRegularFile).toList());
            if (toMove.isEmpty()) {
                return;
            }
            Files.createDirectories(target);
            for (Path file : toMove) {
                Files.move(file, target.resolve(file.getFileName()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not archive the documents of task " + taskNo, e);
        }
    }
}
