package engine.documents;

import engine.shared.ProjectCleanup;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Removes epics, requirements and generated documents of a deleted project. */
@Component
class DocumentCleanup implements ProjectCleanup {

    private final JdbcClient jdbc;

    DocumentCleanup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void deleteAllOf(UUID projectId) {
        for (String table : new String[] {"generated_documents", "requirements", "epics"}) {
            jdbc.sql("delete from documents." + table + " where project_id = :id")
                    .param("id", projectId).update();
        }
    }
}
