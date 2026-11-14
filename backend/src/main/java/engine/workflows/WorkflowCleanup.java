package engine.workflows;

import engine.shared.ProjectCleanup;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Removes the workflow and all contract versions of a deleted project. */
@Component
class WorkflowCleanup implements ProjectCleanup {

    private final JdbcClient jdbc;

    WorkflowCleanup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void deleteAllOf(UUID projectId) {
        jdbc.sql("select set_config('engine.deleting_project', 'on', true)").query().singleValue();
        jdbc.sql("delete from workflows.contracts where project_id = :id")
                .param("id", projectId).update();
        jdbc.sql("delete from workflows.workflows where project_id = :id")
                .param("id", projectId).update();
    }
}
