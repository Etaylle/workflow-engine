package engine.questions;

import engine.shared.ProjectCleanup;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Removes the questions of a deleted project (answers go with them by cascade). */
@Component
class QuestionCleanup implements ProjectCleanup {

    private final JdbcClient jdbc;

    QuestionCleanup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void deleteAllOf(UUID projectId) {
        jdbc.sql("delete from questions.research_sources where project_id = :id")
                .param("id", projectId).update();
        jdbc.sql("delete from questions.questions where project_id = :id")
                .param("id", projectId).update();
    }
}
