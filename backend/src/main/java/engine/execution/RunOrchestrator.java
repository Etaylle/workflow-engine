package engine.execution;

import engine.shared.ProjectPort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final BigDecimal MIN_BUDGET = new BigDecimal("0.30");

    private final RunStore store;
    private final RunEvents events;
    private final Builder builder;
    private final ExecutionSettings settings;
    private final ProjectPort projects;
    private final JdbcClient jdbc;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final List<StepDef> profile = StepProfile.shortProfile();
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "run-orchestrator");
        thread.setDaemon(true);
        return thread;
    });

    RunOrchestrator(RunStore store, RunEvents events, Builder builder, ExecutionSettings settings,
            ProjectPort projects, JdbcClient jdbc, @Value("${spring.datasource.url}") String dbUrl,
            @Value("${spring.datasource.username:}") String dbUser,
            @Value("${spring.datasource.password:}") String dbPassword) {
        this.store = store;
        this.events = events;
        this.builder = builder;
        this.settings = settings;
        this.projects = projects;
        this.jdbc = jdbc;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    List<StepDef> profile() {
        return profile;
    }

    static String databaseName(UUID runId) {
        return "run_" + runId.toString().replace("-", "").substring(0, 12);
    }

    /** Starts the run in the background and returns at once. */
    void submit(UUID runId, String contract) {
        executor.submit(() -> {
            try {
                execute(runId, contract);
            } catch (RuntimeException e) {
                LOG.error("Run {} stopped on an unexpected error", runId, e);
                pause(runId, "The run could not continue: " + e.getMessage());
            }
        });
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
        events.publish(runId, "console", "Preparing the workspace from the template");
        workspace.prepare(settings.templateDir(), contract);
        Map<String, String> appEnv = createDatabase(run);
        store.setRun(runId, "executing", null);
        events.publish(runId, "run", "executing");

        Ctx ctx = new Ctx(run, workspace, logDir, appEnv);
        List<RunStore.TaskRow> tasks = store.tasksToBuild(runId);
        for (int i = 0; i < tasks.size(); i++) {
            if (!buildTask(ctx, contract, tasks, i)) {
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

    private Map<String, String> createDatabase(RunStore.RunRow run) {
        if (!settings.createDatabase()) {
            return Map.of();
        }
        jdbc.sql("create database " + run.databaseName()).update();
        events.publish(run.id(), "console", "Database " + run.databaseName() + " created for the project");
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
    private record Attempt(boolean passed, String text) {
    }

    private boolean buildTask(Ctx ctx, String contract, List<RunStore.TaskRow> tasks, int index) {
        RunStore.TaskRow task = tasks.get(index);
        store.setTask(task.id(), "running");
        events.publish(ctx.run().id(), "task", "Task " + (index + 1) + " of " + tasks.size() + ": " + task.title());
        List<String> done = tasks.subList(0, index).stream().map(RunStore.TaskRow::title).toList();
        List<String> later = tasks.subList(index + 1, tasks.size()).stream().map(RunStore.TaskRow::title).toList();
        ctx.workspace().writeTask(contract, task.title(), task.description(), done, later);
        for (int s = 0; s < task.steps().size(); s++) {
            if (!buildStep(ctx, task, s, index + 1)) {
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
        StepDef def = profile.get(stepIndex);
        store.setStep(row.id(), "running", null);
        events.publish(runId, "step", "Step " + def.title() + " started");
        int attempts = 1 + settings.fixAttempts();
        String failure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            BigDecimal remaining = ctx.run().costLimitUsd().subtract(store.costOf(runId));
            if (remaining.compareTo(MIN_BUDGET) < 0) {
                return failStep(ctx.run(), task, row, "The cost limit of USD " + ctx.run().costLimitUsd()
                        + " is reached; the run waits for the client");
            }
            if (attempt > 1) {
                events.publish(runId, "console", "Fix attempt " + (attempt - 1) + " of "
                        + settings.fixAttempts() + " for " + def.title());
            }
            Attempt result = attempt(ctx, row, def, remaining, failure);
            if (result.passed()) {
                String sha = ctx.workspace().commit("Task " + taskNo + " step " + def.key() + " green");
                store.addCheckpoint(row.id(), sha);
                store.setStep(row.id(), "done", result.text());
                events.publish(runId, "step", "Step " + def.title() + " done");
                return true;
            }
            failure = result.text();
            events.publish(runId, "console", "Attempt " + attempt + " failed: " + failure);
        }
        return failStep(ctx.run(), task, row, failure);
    }

    private Attempt attempt(Ctx ctx, RunStore.StepRow row, StepDef def, BigDecimal remaining,
            String previousFailure) {
        UUID runId = ctx.run().id();
        String prompt = previousFailure == null ? def.prompt() : def.prompt()
                + "\n\nYour previous attempt at this step failed. What went wrong:\n" + previousFailure
                + "\n\nFix this. Change only what is needed, then run ./check.sh until it prints"
                + " CHECK PASSED.";
        StepOutcome outcome = builder.run(new StepRequest(ctx.workspace().dir(), def.key(), prompt,
                def.capUsd().min(remaining), ctx.appEnv(), ctx.logDir()));
        UUID call = store.addAgentCall(row.id(), outcome);
        events.publish(runId, "console", def.title() + ": " + outcome.summary() + " (USD "
                + outcome.costUsd().setScale(2, java.math.RoundingMode.HALF_UP) + ", "
                + outcome.turns() + " turns)");
        if (!outcome.ok()) {
            return new Attempt(false, def.title() + " failed: " + outcome.summary());
        }
        if (def.gated()) {
            GateResult gate = builder.gate(ctx.workspace().dir(), ctx.appEnv(), ctx.logDir());
            gate.tail().forEach(line -> events.publish(runId, "console", "check: " + line));
            if (!gate.passed()) {
                String text = "The check after " + def.title() + " did not pass:\n"
                        + String.join("\n", gate.tail());
                store.markCheckFailed(call, text);
                return new Attempt(false, text);
            }
        }
        return new Attempt(true, outcome.summary());
    }

    private boolean failStep(RunStore.RunRow run, RunStore.TaskRow task, RunStore.StepRow step,
            String reason) {
        store.setStep(step.id(), "failed", reason);
        store.setTask(task.id(), "failed");
        pause(run.id(), reason);
        return false;
    }

    private void pause(UUID runId, String reason) {
        store.setRun(runId, "paused", reason);
        events.publish(runId, "run", "paused: " + reason);
    }

    /** Moves the documents of a finished task into docs/task-N so the next task starts clean. */
    private static void archiveDocs(Path dir, int taskNo) {
        Path docs = dir.resolve("docs");
        Path target = docs.resolve("task-" + taskNo);
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
