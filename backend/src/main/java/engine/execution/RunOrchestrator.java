package engine.execution;

import engine.shared.ProjectPort;
import engine.shared.Template;
import engine.shared.TemplateCatalog;
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
    private final TemplateCatalog templates;
    private final UseCheck useCheck;
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
            ProjectPort projects, JdbcClient jdbc, TemplateCatalog templates, UseCheck useCheck,
            @Value("${spring.datasource.url}") String dbUrl,
            @Value("${spring.datasource.username:}") String dbUser,
            @Value("${spring.datasource.password:}") String dbPassword) {
        this.store = store;
        this.events = events;
        this.builders = builders;
        this.settings = settings;
        this.projects = projects;
        this.jdbc = jdbc;
        this.templates = templates;
        this.useCheck = useCheck;
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
        Template template = templates.require(run.templateId());
        Map<String, String> appEnv;
        if ("preparing".equals(run.status())) {
            events.publish(runId, "console", "Preparing the workspace from the template");
            workspace.prepare(template.dir(), contract);
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
        Ctx ctx = new Ctx(run, template, workspace, logDir, appEnv);
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
        if (run.databaseName() != null) {
            jdbc.sql("create database " + run.databaseName()).update();
            events.publish(run.id(), "console", "Database " + run.databaseName() + " created for the project");
        }
    }

    private Map<String, String> appEnv(RunStore.RunRow run) {
        if (run.databaseName() == null) {
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
    private record Ctx(RunStore.RunRow run, Template template, Workspace workspace, Path logDir, Map<String, String> appEnv) {
    }

    /**
     * The result of one attempt at a step: passed with a summary, or failed with what went wrong.
     * A failure the builder cannot fix by itself (a tool the owner must install) asks the client.
     */
    private record Attempt(Kind kind, String text, String session) {

        /** How an attempt ended; RUN_LIMIT means the cost limit of the run cut it short. */
        enum Kind { PASSED, FAILED, LIMITED, ASK, RUN_LIMIT }

        static Attempt passed(String summary) {
            return new Attempt(Kind.PASSED, summary, null);
        }

        static Attempt failed(String text, String session) {
            return new Attempt(Kind.FAILED, text, session);
        }

        static Attempt ask(String question) {
            return new Attempt(Kind.ASK, question, null);
        }

        boolean passed() {
            return kind == Kind.PASSED;
        }
    }

    /** The file in which the builder says what the owner must install on the machine. */
    static final String NEEDS_FILE = "NEEDS.md";
    private static final int NEEDS_LENGTH = 1500;

    /** Added to the prompt of a review step that a separate reviewer builder runs (D-103). */
    static final String REVIEWER_NOTE = "\n\nYou are reviewing work written by another AI builder. Judge it"
            + " independently, do not assume it is right, fix real defects yourself where this step allows"
            + " changing code, and note in docs/CHECK.md (or the document this step writes) what you"
            + " changed.";

    private boolean buildTask(Ctx ctx, String contract, List<RunStore.TaskRow> tasks, int index) {
        RunStore.TaskRow task = tasks.get(index);
        store.setTask(task.id(), "running");
        events.publish(ctx.run().id(), "task", "Task " + (index + 1) + " of " + tasks.size() + ": " + task.title());
        List<String> done = tasks.subList(0, index).stream().map(RunStore.TaskRow::title).toList();
        List<String> later = RunService.FOUNDATION_TITLE.equals(task.title()) ? List.of()
                : tasks.subList(index + 1, tasks.size()).stream().map(RunStore.TaskRow::title).toList();
        if ("whole".equals(ctx.run().buildMode())) {
            ctx.workspace().writeWhole(contract, ctx.template(), task.description());
        } else {
            ctx.workspace().writeTask(contract, ctx.template(), task.title(), task.description(), done,
                    later);
        }
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

    /**
     * Runs a step; a failed attempt goes back to the builder with the failure, a bounded number of
     * times. Where the builder keeps a session, the next attempt continues it instead of starting
     * over, so what the builder already read and wrote is not paid for twice.
     */
    private boolean buildStep(Ctx ctx, RunStore.TaskRow task, int stepIndex, int taskNo) {
        UUID runId = ctx.run().id();
        RunStore.StepRow row = task.steps().get(stepIndex);
        Step def = withTask(ctx.run().steps().get(stepIndex), task.title());
        store.setStep(row.id(), "running", null);
        events.publish(runId, "step", "Step " + def.title() + " started");
        int attempts = 1 + settings.fixAttempts();
        Tried last = new Tried(null, null);
        int waits = 0;
        int attempt = 1;
        while (attempt <= attempts) {
            if (stopped(runId)) {
                return false;
            }
            BigDecimal limit = store.costLimitOf(runId).orElse(null);
            BigDecimal remaining = limit == null ? null : limit.subtract(store.costOf(runId));
            if (remaining != null && remaining.compareTo(MIN_BUDGET) < 0) {
                return costLimit(runId, limit, task, row);
            }
            announceFix(runId, def, attempt, waits);
            Attempt result = attempt(ctx, row, def, taskNo, remaining, last);
            if (stopped(runId)) {
                return false;
            }
            switch (result.kind()) {
                case PASSED:
                    return completeStep(ctx, row, def, taskNo, result.text());
                case ASK:
                    return askClient(ctx.run(), task, row, result.text(), result.text());
                case RUN_LIMIT:
                    return costLimit(runId, limit, task, row);
                case LIMITED:
                    if (!waitForTokens(ctx, task, row, result.text(), ++waits)) {
                        return false;
                    }
                    continue;
                case FAILED:
                    break;
            }
            waits = 0;
            last = new Tried(result.text(), result.session());
            events.publish(runId, "console", "Attempt " + attempt + " failed: " + last.failure());
            attempt++;
        }
        return askClient(ctx.run(), task, row, def.title() + " could not be fixed after " + attempts
                + " attempts. What should the engine do? Last failure: " + last.failure(), last.failure());
    }

    /** What the previous attempt at a step left: why it failed and the session to continue, if any. */
    private record Tried(String failure, String session) {
    }

    /** The prompt of a step may name the task as {task}. */
    private static Step withTask(Step step, String taskTitle) {
        return new Step(step.key(), step.title(), step.gated(), step.capUsd(),
                step.prompt().replace("{task}", taskTitle), step.review(), step.reviewerOnly(),
                step.effort());
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

    /**
     * The cap of one invocation, or null without one (the default): the cap of the step, in whole mode
     * a multiple of it, and never more than what the cost limit of the run leaves.
     */
    private BigDecimal capOf(RunStore.RunRow run, Step def, BigDecimal remaining) {
        if (def.capUsd() == null) {
            return remaining;
        }
        BigDecimal cap = "whole".equals(run.buildMode())
                ? def.capUsd().multiply(settings.wholeCapMultiple()) : def.capUsd();
        return remaining == null ? cap : cap.min(remaining);
    }

    /** The effort a step runs with: its own when it has one, in the levels the builder knows. */
    static String effortOf(engine.shared.Harness harness, String stepEffort, String runEffort) {
        if (stepEffort == null) {
            return runEffort;
        }
        return switch (harness) {
            case GEMINI_CLI -> null;
            case CODEX_CLI -> "max".equals(stepEffort) ? "xhigh" : stepEffort;
            case CLAUDE_CODE -> stepEffort;
        };
    }

    /** What a new attempt is told: the step, and after a failure what went wrong, then the files. */
    private String promptOf(Ctx ctx, RunStore.StepRow row, Step def, Tried last, boolean separate) {
        if (last.session() != null) {
            return "Your attempt at this step did not end green. What went wrong:\n" + last.failure()
                    + "\n\nGo on from where you are: fix this, change only what is needed, then run"
                    + " ./check.sh until it prints CHECK PASSED. Finish with one line saying what you did.";
        }
        String base = hintsOf(row, def);
        String prompt = last.failure() == null ? base : base
                + "\n\nYour previous attempt at this step failed. What went wrong:\n" + last.failure()
                + "\n\nFix this. Change only what is needed, then run ./check.sh until it prints"
                + " CHECK PASSED.";
        return (separate ? prompt + REVIEWER_NOTE : prompt) + ctx.workspace().fileMap();
    }

    private Attempt attempt(Ctx ctx, RunStore.StepRow row, Step def, int taskNo, BigDecimal remaining,
            Tried last) {
        UUID runId = ctx.run().id();
        boolean separate = def.review() && ctx.run().reviewer() != null;
        BuilderRun by = separate ? ctx.run().reviewer() : new BuilderRun(ctx.run().harness(),
                ctx.run().builderModel(),
                effortOf(ctx.run().harness(), def.effort(), ctx.run().builderEffort()));
        Builder builder = builders.forHarness(by.harness());
        BigDecimal cap = capOf(ctx.run(), def, remaining);
        String logName = String.format("task-%02d-%s-attempt-%d", taskNo, def.key(),
                store.nextAttempt(row.id()));
        StepOutcome outcome = builder.run(new StepRequest(ctx.workspace().dir(), def.key(),
                promptOf(ctx, row, def, last, separate), cap, by.model(), by.effort(), ctx.appEnv(),
                ctx.logDir(), () -> stopped(runId), line -> events.publish(runId, "console", line),
                logName, last.session()));
        UUID call = store.addAgentCall(row.id(), outcome, by);
        events.publish(runId, "console", def.title() + ": " + outcome.summary() + " (USD "
                + outcome.costUsd().setScale(2, java.math.RoundingMode.HALF_UP) + ", "
                + outcome.turns() + " turns)");
        String needs = BuilderSupport.read(ctx.workspace().dir().resolve(NEEDS_FILE)).strip();
        if (!needs.isEmpty()) {
            return Attempt.ask("The builder needs something installed on this machine"
                    + " before it can continue. Install it and answer Retry, or stop the run. It says: "
                    + (needs.length() > NEEDS_LENGTH ? needs.substring(0, NEEDS_LENGTH) + "..." : needs));
        }
        if (outcome.overBudget()) {
            return overBudget(def, outcome, cap, remaining);
        }
        if (!outcome.ok()) {
            return new Attempt(outcome.limited() ? Attempt.Kind.LIMITED : Attempt.Kind.FAILED,
                    def.title() + " failed: " + outcome.summary(), outcome.session());
        }
        return def.gated() ? gates(ctx, def, builder, call, outcome)
                : Attempt.passed(outcome.summary());
    }

    /**
     * A budget cut the step short. Sending it back to the builder would only spend the same again, so
     * the run waits: at the cost limit of the run when that was the budget, else with a question
     * that names the step budget.
     */
    private static Attempt overBudget(Step def, StepOutcome outcome, BigDecimal cap, BigDecimal remaining) {
        if (remaining != null && cap != null && cap.compareTo(remaining) == 0) {
            return new Attempt(Attempt.Kind.RUN_LIMIT, outcome.summary(), null);
        }
        return Attempt.ask(def.title() + ": " + outcome.summary() + ". Nothing is wrong with the work;"
                + " the budget of this step was too small for it. Answer Retry to run the step again"
                + " with the same budget, Skip to go on without it, or stop the run. For more room,"
                + " raise or clear the cost cap of this step in the step profile (Team and steps) and"
                + " start a new run.");
    }

    /** The gates of a step: ./check.sh must pass, then the software must work when it is used. */
    private Attempt gates(Ctx ctx, Step def, Builder builder, UUID call, StepOutcome outcome) {
        UUID runId = ctx.run().id();
        GateResult gate = builder.gate(ctx.workspace().dir(), ctx.appEnv(), ctx.logDir(),
                line -> events.publish(runId, "console", line));
        if (!gate.passed()) {
            String text = "The check after " + def.title() + " did not pass:\n"
                    + String.join("\n", gate.tail());
            store.markCheckFailed(call, text);
            return Attempt.failed(text, outcome.session());
        }
        if (useCheck.enabled() && !builders.demo()) {
            UseCheck.Result use = useCheck.run(ctx.template(), ctx.workspace().dir(), ctx.appEnv(),
                    ctx.logDir(), () -> stopped(runId), line -> events.publish(runId, "console", line));
            if (!use.passed() && !stopped(runId)) {
                String text = "./check.sh passed after " + def.title() + ", but the software does not"
                        + " work when it is started and used the way a client would:\n"
                        + String.join("\n", use.lines())
                        + "\nMake it work for a real user, and add a test that would have caught this.";
                store.markCheckFailed(call, text);
                return Attempt.failed(text, outcome.session());
            }
        }
        return Attempt.passed(outcome.summary());
    }

    /** Not fixable after the attempts: the run waits with one question for the client. */
    private boolean askClient(RunStore.RunRow run, RunStore.TaskRow task, RunStore.StepRow step,
            String question, String failure) {
        store.setStep(step.id(), "failed", failure);
        store.setTask(task.id(), "failed");
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
        store.row(runId).ifPresent(run -> projects.markStatus(run.projectId(), "confirmed"));
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
