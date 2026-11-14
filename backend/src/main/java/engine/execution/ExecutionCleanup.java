package engine.execution;

import engine.shared.ProjectCleanup;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Removes the runs of a deleted project: rows, workspace folders and the databases of the runs. */
@Component
class ExecutionCleanup implements ProjectCleanup {

    private final RunStore store;
    private final JdbcClient jdbc;

    ExecutionCleanup(RunStore store, JdbcClient jdbc) {
        this.store = store;
        this.jdbc = jdbc;
    }

    @Override
    public void deleteAllOf(UUID projectId) {
        for (RunStore.RunRow run : store.runsOf(projectId)) {
            Path workspace = Path.of(run.workspacePath());
            deleteTree(workspace);
            deleteTree(RunOrchestrator.logDir(new Workspace(workspace)));
            if (run.databaseName() != null) {
                jdbc.sql("drop database if exists " + run.databaseName() + " with (force)").update();
            }
        }
        store.deleteAllOf(projectId);
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
            throw new UncheckedIOException("Could not delete " + root, e);
        }
    }
}
