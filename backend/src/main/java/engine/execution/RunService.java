package engine.execution;

import engine.shared.ApiProblem;
import engine.shared.ContractPort;
import engine.shared.ProjectPort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    private final Builder builder;
    private final ProjectPort projects;
    private final ContractPort contracts;
    private final ExecutionSettings settings;

    RunService(RunStore store, RunOrchestrator orchestrator, RunEvents events, Builder builder,
            ProjectPort projects, ContractPort contracts, ExecutionSettings settings) {
        this.store = store;
        this.orchestrator = orchestrator;
        this.events = events;
        this.builder = builder;
        this.projects = projects;
        this.contracts = contracts;
        this.settings = settings;
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
        builder.checkReady();
        UUID runId = UUID.randomUUID();
        Path workspace = settings.workspaceRoot().resolve("run-" + runId);
        store.create(runId, projectId, contract.version(),
                contract.costLimitUsd().orElse(settings.costLimitUsd()), workspace.toString(),
                settings.createDatabase() ? RunOrchestrator.databaseName(runId) : null,
                contract.softwareTasks(), orchestrator.profile());
        projects.markStatus(projectId, "executing");
        orchestrator.submit(runId, contract.markdown());
        return view(runId);
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

    SseEmitter stream(UUID runId, long after) {
        view(runId);
        return events.subscribe(runId, after);
    }

    /** The workspace of a run that is not being built, for the ZIP. */
    Workspace workspaceForZip(UUID runId) {
        RunView run = view(runId);
        if ("preparing".equals(run.status()) || "executing".equals(run.status())) {
            throw new ApiProblem(409, "Run under way", "The ZIP is available when the run has ended.");
        }
        Path dir = Path.of(store.row(runId).orElseThrow().workspacePath());
        if (!Files.isDirectory(dir)) {
            throw new ApiProblem(404, "Workspace gone", "The workspace of this run no longer exists.");
        }
        return new Workspace(dir);
    }

    /** After a restart no run is under way any more: the key and the build process are gone. */
    @EventListener(ApplicationReadyEvent.class)
    void recoverInterrupted() {
        for (UUID runId : store.interruptActive()) {
            events.publish(runId, "run", "interrupted: the engine restarted");
        }
    }
}
