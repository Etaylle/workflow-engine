package engine.execution;

import engine.shared.ApiProblem;
import engine.shared.ContractPort;
import engine.shared.BuilderChoice;
import engine.shared.Harness;
import engine.shared.ProjectPort;
import engine.shared.ReviewerChoice;
import engine.shared.RunSettingsPort;
import engine.shared.Template;
import engine.shared.TemplateCatalog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Starts runs on confirmed contracts and answers about them. */
@Service
class RunService {

    private final RunStore store;
    private final RunOrchestrator orchestrator;
    private final RunEvents events;
    private final Builders builders;
    private final ProjectPort projects;
    private final ContractPort contracts;
    private final ExecutionSettings settings;
    private final RunSettingsPort runSettings;
    private final TemplateCatalog templates;

    RunService(RunStore store, RunOrchestrator orchestrator, RunEvents events, Builders builders,
            ProjectPort projects, ContractPort contracts, ExecutionSettings settings,
            RunSettingsPort runSettings, TemplateCatalog templates) {
        this.templates = templates;
        this.runSettings = runSettings;
        this.store = store;
        this.orchestrator = orchestrator;
        this.events = events;
        this.builders = builders;
        this.projects = projects;
        this.contracts = contracts;
        this.settings = settings;
    }

    /** Software with at most this many software tasks is built as one unit unless the client chose. */
    static final int WHOLE_MAX_TASKS = 8;
    static final String WHOLE_TITLE = "Build the whole software of the contract";

    static String defaultBuildMode(int softwareTasks) {
        return softwareTasks <= WHOLE_MAX_TASKS ? "whole" : "tasks";
    }

    /** The one unit of work of the whole mode: every software and manual task is its scope. */
    private static ContractPort.TaskLine wholeUnit(ContractPort.ContractInfo contract) {
        StringBuilder scope = new StringBuilder("The whole software of the contract. Software tasks: ");
        scope.append(titles(contract.softwareTasks()));
        if (!contract.manualTasks().isEmpty()) {
            scope.append(". Steps done by a person, which the software must offer as user actions: ")
                    .append(titles(contract.manualTasks()));
        }
        return new ContractPort.TaskLine(WHOLE_TITLE, "all", scope.toString());
    }

    static final String FOUNDATION_TITLE = "A working whole in plain form";
    static final String FINAL_TITLE = "Final pass against the wish";
    static final int FRAMED_MIN_TASKS = 3;

    /**
     * The units of work of a run. Whole mode: one unit. Task by task from three tasks on: first the
     * whole software in its plainest working form, so that every checkpoint after it is usable from
     * start to end, then one unit per task that deepens its part, then a pass over the whole against
     * the wish, which catches what no single task owns. With fewer tasks the two extra units would
     * double the run, so the tasks stand alone.
     */
    static List<ContractPort.TaskLine> units(ContractPort.ContractInfo contract, String buildMode) {
        if ("whole".equals(buildMode)) {
            return List.of(wholeUnit(contract));
        }
        if (contract.softwareTasks().size() < FRAMED_MIN_TASKS) {
            return contract.softwareTasks();
        }
        List<ContractPort.TaskLine> units = new java.util.ArrayList<>();
        units.add(new ContractPort.TaskLine(FOUNDATION_TITLE, "all", "Before the single tasks: build the"
                + " whole software of the contract in its plainest working form, so that a user can"
                + " already go through it from start to end (for a game: start, play, win or lose, start"
                + " again). Every task must be there in a simple form: " + titles(contract.softwareTasks())
                + (contract.manualTasks().isEmpty() ? "" : "; and the steps done by a person, as user"
                        + " actions: " + titles(contract.manualTasks()))
                + ". Leave detail, depth and polish to the tasks that follow, which each deepen one part,"
                + " and keep everything the wish does not ask for as small as it can be."));
        units.addAll(contract.softwareTasks());
        units.add(new ContractPort.TaskLine(FINAL_TITLE, "all", "After all tasks: go through the whole"
                + " software from start to end as a critical client would, against the ORIGINAL WISH"
                + " (section Wish) and, when the wish names software with a well-known design, against"
                + " every part of that design a user expects. Write down what is missing, wrong or"
                + " unfinished when the software is used, then build and fix exactly that. Add nothing the"
                + " wish does not ask for."));
        return List.copyOf(units);
    }

    private static String titles(List<ContractPort.TaskLine> tasks) {
        return String.join("; ", tasks.stream().map(ContractPort.TaskLine::title).toList());
    }

    RunView start(UUID projectId) {
        String status = projects.statusOf(projectId).orElseThrow(
                () -> new ApiProblem(404, "Project not found", "No project " + projectId));
        if (!Set.of("confirmed", "executing", "done").contains(status)) {
            throw new ApiProblem(409, "Not confirmed",
                    "A run starts only on a confirmed contract. Confirm the workflow first.");
        }
        if (store.projectHasActiveRun(projectId)) {
            throw new ApiProblem(409, "Run under way", "This project already has a run under way.");
        }
        ContractPort.ContractInfo contract = contracts.latestContract(projectId).orElseThrow(
                () -> new ApiProblem(409, "No contract", "The project has no confirmed contract."));
        if (contract.softwareTasks().isEmpty()) {
            throw new ApiProblem(409, "No software task",
                    "The workflow has no software task to build. Mark a task as software first.");
        }
        Harness harness = projects.harnessOf(projectId).orElseThrow();
        Optional<ReviewerChoice> reviewer = projects.reviewerOf(projectId);
        checkReady(harness, reviewer.map(ReviewerChoice::harness).orElse(null));
        Template template = templates.require(projects.templateIdOf(projectId).orElseThrow());
        UUID runId = UUID.randomUUID();
        String buildMode = projects.buildModeOf(projectId)
                .orElse(defaultBuildMode(contract.softwareTasks().size()));
        BuilderChoice choice = projects.builderChoiceOf(projectId).orElseThrow();
        BuilderRun reviewerRun = reviewer.map(r -> new BuilderRun(r.harness(),
                resolvedModel(r.harness(), r.choice()), r.choice().effort())).orElse(null);
        Path workspace = settings.workspaceRoot().resolve("run-" + runId);
        RunSettingsPort.RunSettings chosen = runSettings.forProject(projectId);
        store.create(runId, projectId, contract.version(), harness,
                chosen.costLimitUsd().or(contract::costLimitUsd).orElse(null),
                workspace.toString(), template.id(),
                settings.createDatabase() && template.needsDatabase() ? RunOrchestrator.databaseName(runId) : null,
                buildMode, resolvedModel(harness, choice), choice.effort(), reviewerRun,
                units(contract, buildMode),
                chosen.stepsFor(reviewerRun != null));
        projects.markStatus(projectId, "executing");
        orchestrator.submit(runId, contract.markdown());
        return view(runId);
    }

    /** The model a run snapshots: the project's choice, else the default of the builder. */
    private String resolvedModel(Harness harness, BuilderChoice choice) {
        if (choice.model() != null) {
            return choice.model();
        }
        return switch (harness) {
            case GEMINI_CLI -> settings.geminiModel();
            case CODEX_CLI -> settings.codexModel();
            case CLAUDE_CODE -> settings.model();
        };
    }

    /** Both builders of a run must be ready before it starts or continues; the problem names which one is not. */
    private void checkReady(Harness main, Harness reviewer) {
        builders.forHarness(main).checkReady();
        if (reviewer == null) {
            return;
        }
        try {
            builders.forHarness(reviewer).checkReady();
        } catch (ApiProblem e) {
            throw new ApiProblem(e.status(), "Reviewer not ready: " + reviewer.label(),
                    "The reviewer is not ready. " + e.getMessage(), e);
        }
    }

    RunView view(UUID runId) {
        return store.view(runId).orElseThrow(
                () -> new ApiProblem(404, "Run not found", "No run " + runId));
    }

    RunView latest(UUID projectId) {
        projects.statusOf(projectId).orElseThrow(
                () -> new ApiProblem(404, "Project not found", "No project " + projectId));
        return view(store.latestRunId(projectId).orElseThrow(
                () -> new ApiProblem(404, "No run", "The project has no run yet.")));
    }

    List<RunEvent> events(UUID runId, long after) {
        view(runId);
        return store.events(runId, after, 1000);
    }

    /** The latest events below a number, for scrolling back in the console. */
    List<RunEvent> eventsBefore(UUID runId, long before, String kind, int limit) {
        view(runId);
        return store.eventsBefore(runId, before, kind, Math.max(1, Math.min(limit, 1000)));
    }

    SseEmitter stream(UUID runId, long after) {
        view(runId);
        return events.subscribe(runId, after);
    }

    /** The client answers the question of a waiting run; the run continues with the answer. */
    RunView answer(UUID runId, UUID questionId, String choice, String hint) {
        RunView run = view(runId);
        RunStore.QuestionRow open = store.openQuestion(runId).filter(q -> "question".equals(run.waitKind()))
                .filter(q -> q.id().equals(questionId)).orElseThrow(() -> new ApiProblem(409,
                        "No such question", "The run is not waiting for an answer to this question."));
        if (!List.of("retry", "skip", "stop").contains(choice)) {
            throw new ApiProblem(400, "Unknown answer", "Choose retry, skip or stop.");
        }
        String text = hint == null ? null : hint.strip();
        if (text != null && text.length() > 1000) {
            throw new ApiProblem(400, "Hint too long", "Keep the hint under 1000 characters.");
        }
        if ("stop".equals(choice)) {
            store.answerQuestion(open.id(), choice, text);
            return stop(runId);
        }
        checkReady(run);
        store.answerQuestion(open.id(), choice, text);
        if ("skip".equals(choice)) {
            store.setStep(open.stepId(), "done", "Skipped by the client");
        }
        events.publish(runId, "run", "answered: " + choice);
        continueRun(runId, run.projectId(), run.contractVersion());
        return view(runId);
    }

    /**
     * The client stops the run at any time: it ends as failed (the only way to failed), a build under
     * way is ended, and the workspace goes back to the last green checkpoint (the ZIP shows it).
     */
    RunView stop(UUID runId) {
        RunView run = view(runId);
        if (!Set.of("preparing", "executing", "waiting", "interrupted").contains(run.status())) {
            throw new ApiProblem(409, "Run has ended", "This run has already ended; there is nothing to stop.");
        }
        store.setRun(runId, "failed", "Stopped by the client");
        store.failUnfinished(runId, "Stopped by the client");
        events.publish(runId, "run", "failed: stopped by the client");
        projects.markStatus(run.projectId(), "confirmed");
        orchestrator.stop(runId);
        return view(runId);
    }

    /**
     * Continues a run from the last checkpoint: an interrupted one (after an engine restart the key
     * is asked for again first, and the resumes are bounded), one that stopped waiting for tokens, or
     * one that reached the cost limit (then {@code raiseLimitTo} must lift the limit). With
     * {@code withoutReviewer} the run drops its separate reviewer for good (D-112): the builder runs
     * the review steps that are left and the second opinion on the plan is left out.
     */
    RunView resume(UUID runId, BigDecimal raiseLimitTo, boolean withoutReviewer) {
        RunView run = view(runId);
        boolean interrupted = "interrupted".equals(run.status());
        boolean waiting = "waiting".equals(run.status());
        if (!interrupted && !(waiting && Set.of("tokens", "cost_limit").contains(run.waitKind()))) {
            throw new ApiProblem(409, "Cannot continue", "Only an interrupted run, a run waiting for tokens"
                    + " or a run at its cost limit can be continued; answer the question of a waiting run.");
        }
        if (orchestrator.isBuilding(runId)) {
            throw new ApiProblem(409, "Run retrying", "The run is waiting for tokens and tries again by"
                    + " itself; continue it only when it has stopped retrying.");
        }
        if (withoutReviewer && run.reviewerHarness() != null) {
            store.clearReviewer(runId);
            events.publish(runId, "run", "The run goes on without the second opinion of "
                    + Harness.ofId(run.reviewerHarness()).label() + ": the builder runs the review steps.");
            run = view(runId);
        }
        checkReady(run);
        if ("cost_limit".equals(run.waitKind()) || raiseLimitTo != null) {
            raiseLimit(run, raiseLimitTo);
        }
        if (interrupted) {
            countResume(run);
        }
        events.publish(runId, "run", "continued by the client");
        continueRun(runId, run.projectId(), run.contractVersion());
        return view(runId);
    }

    /** The builders of the run: a resume never switches builders, it can only drop the reviewer. */
    private void checkReady(RunView run) {
        checkReady(Harness.ofId(run.harness()),
                run.reviewerHarness() == null ? null : Harness.ofId(run.reviewerHarness()));
    }

    /** A run at its cost limit continues only with a higher one. */
    private void raiseLimit(RunView run, BigDecimal raiseLimitTo) {
        BigDecimal least = run.costUsd().add(RunOrchestrator.MIN_BUDGET);
        if (raiseLimitTo == null || raiseLimitTo.compareTo(least) < 0) {
            throw new ApiProblem(400, "Raise the limit", "The cost so far is USD " + run.costUsd()
                    .setScale(2, RoundingMode.HALF_UP) + ". Give a limit of at least USD "
                    + least.setScale(2, RoundingMode.CEILING) + " to continue.");
        }
        store.setCostLimit(run.id(), raiseLimitTo);
    }

    private void countResume(RunView run) {
        if (store.resumeCount(run.id()) >= settings.maxResumes()) {
            store.setRun(run.id(), "failed", "The run was resumed " + settings.maxResumes()
                    + " times and was interrupted again.");
            projects.markStatus(run.projectId(), "confirmed");
            events.publish(run.id(), "run", "failed: resumed " + settings.maxResumes() + " times already");
            throw new ApiProblem(409, "Too many resumes", "This run was resumed " + settings.maxResumes()
                    + " times already. Start a new run; the ZIP of the last checkpoint is available.");
        }
        store.addResume(run.id());
        events.publish(run.id(), "run", "resumed (" + store.resumeCount(run.id()) + " of "
                + settings.maxResumes() + ")");
    }

    private void continueRun(UUID runId, UUID projectId, int contractVersion) {
        ContractPort.ContractInfo contract = contracts.latestContract(projectId).orElseThrow(
                () -> new ApiProblem(409, "No contract", "The project has no confirmed contract."));
        if (contract.version() != contractVersion) {
            throw new ApiProblem(409, "Contract changed", "The contract was confirmed again since this run"
                    + " started. Start a new run on the new contract.");
        }
        store.setRun(runId, "executing", null);
        projects.markStatus(projectId, "executing");
        orchestrator.submit(runId, contract.markdown());
    }

    /** The final report of a run (what is known so far while it is under way). */
    RunReport report(UUID runId) {
        return RunReport.of(view(runId), store.questionsOf(runId), java.time.Instant.now());
    }

    /** The workspace of a run that is not being built, for the ZIP. */
    Workspace workspaceForZip(UUID runId) {
        RunView run = view(runId);
        if ("preparing".equals(run.status()) || "executing".equals(run.status())
                || orchestrator.isBuilding(runId)) {
            throw new ApiProblem(409, "Run under way", "The ZIP is available when the run has ended.");
        }
        Path dir = Path.of(store.row(runId).orElseThrow().workspacePath());
        if (!Files.isDirectory(dir)) {
            throw new ApiProblem(404, "Workspace gone", "The workspace of this run no longer exists.");
        }
        return new Workspace(dir);
    }

    /** The read-only view of what the run built so far (its git history); any run with a workspace. */
    WorkspaceFiles filesOf(UUID runId) {
        view(runId);
        Path dir = Path.of(store.row(runId).orElseThrow().workspacePath());
        if (!Files.isDirectory(dir.resolve(".git"))) {
            throw new ApiProblem(404, "Workspace gone", "The workspace of this run no longer exists.");
        }
        return new WorkspaceFiles(new Workspace(dir));
    }

    /** After a restart no run is under way any more: the key and the build process are gone. */
    @EventListener(ApplicationReadyEvent.class)
    void recoverInterrupted() {
        for (UUID runId : store.interruptActive()) {
            events.publish(runId, "run", "interrupted: the engine restarted");
            store.row(runId).ifPresent(run -> projects.markStatus(run.projectId(), "confirmed"));
        }
    }
}
