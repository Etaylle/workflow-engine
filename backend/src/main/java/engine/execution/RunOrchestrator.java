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

        List<RunStore.TaskRow> tasks = store.tasksToBuild(runId);
        for (int i = 0; i < tasks.size(); i++) {
            if (!buildTask(run, workspace, logDir, appEnv, contract, tasks, i)) {
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

    private boolean buildTask(RunStore.RunRow run, Workspace workspace, Path logDir,
            Map<String, String> appEnv, String contract, List<RunStore.TaskRow> tasks, int index) {
        RunStore.TaskRow task = tasks.get(index);
        store.setTask(task.id(), "running");
        events.publish(run.id(), "task", "Task " + (index + 1) + " of " + tasks.size() + ": " + task.title());
        List<String> done = tasks.subList(0, index).stream().map(RunStore.TaskRow::title).toList();
        List<String> later = tasks.subList(index + 1, tasks.size()).stream().map(RunStore.TaskRow::title).toList();
        workspace.writeTask(contract, task.title(), task.description(), done, later);
        for (int s = 0; s < task.steps().size(); s++) {
            if (!buildStep(run, workspace, logDir, appEnv, task, s, index + 1)) {
                return false;
            }
        }
        archiveDocs(workspace.dir(), index + 1);
        workspace.commit("Task " + (index + 1) + " done: " + task.title());
        store.setTask(task.id(), "done");
        events.publish(run.id(), "task", "Task " + (index + 1) + " done: " + task.title());
        return true;
    }

    private boolean buildStep(RunStore.RunRow run, Workspace workspace, Path logDir,
            Map<String, String> appEnv, RunStore.TaskRow task, int stepIndex, int taskNo) {
        RunStore.StepRow row = task.steps().get(stepIndex);
        StepDef def = profile.get(stepIndex);
        BigDecimal remaining = run.costLimitUsd().subtract(store.costOf(run.id()));
        if (remaining.compareTo(MIN_BUDGET) < 0) {
            return failStep(run, task, row, "The cost limit of USD " + run.costLimitUsd()
                    + " is reached; the run waits for the client");
        }
        store.setStep(row.id(), "running", null);
        events.publish(run.id(), "step", "Step " + def.title() + " started");
        StepOutcome outcome = builder.run(new StepRequest(workspace.dir(), def.key(), def.prompt(),
                def.capUsd().min(remaining), appEnv, logDir));
        store.addAgentCall(row.id(), 1, outcome);
        events.publish(run.id(), "console", def.title() + ": " + outcome.summary() + " (USD "
                + outcome.costUsd().setScale(2, java.math.RoundingMode.HALF_UP) + ", "
                + outcome.turns() + " turns)");
        if (!outcome.ok()) {
            return failStep(run, task, row, def.title() + " failed: " + outcome.summary());
        }
        if (def.gated()) {
            GateResult gate = builder.gate(workspace.dir(), appEnv, logDir);
            gate.tail().forEach(line -> events.publish(run.id(), "console", "check: " + line));
            if (!gate.passed()) {
                return failStep(run, task, row, "The check after " + def.title() + " did not pass");
            }
        }
        String sha = workspace.commit("Task " + taskNo + " step " + def.key() + " green");
        store.addCheckpoint(row.id(), sha);
        store.setStep(row.id(), "done", outcome.summary());
        events.publish(run.id(), "step", "Step " + def.title() + " done");
        return true;
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
