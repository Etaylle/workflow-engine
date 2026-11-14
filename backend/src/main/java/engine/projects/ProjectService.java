package engine.projects;

import engine.shared.ApiProblem;
import engine.shared.ProjectPort;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Creates and reads projects; also the answer of the domain to {@link ProjectPort}. */
@Service
public class ProjectService implements ProjectPort {

    static final int MAX_WISH_LENGTH = 20_000;
    private static final int NAME_LENGTH = 60;
    private static final String COLUMNS = "id, name, wish, status, created_at, updated_at";

    private final JdbcClient jdbc;
    private final Clock clock;

    public ProjectService(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Creates a draft project from the wish; the name is the start of the wish. */
    public Project create(String wish) {
        String trimmed = wish == null ? "" : wish.strip();
        if (trimmed.isEmpty()) {
            throw new ApiProblem(
                    400, "Wish missing", "Describe what you want built; the wish is empty.");
        }
        if (trimmed.length() > MAX_WISH_LENGTH) {
            throw new ApiProblem(
                    400, "Wish too long", "The wish has more than " + MAX_WISH_LENGTH + " characters.");
        }
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.sql(
                        "insert into projects.projects (id, name, wish, status, agent_harness,"
                                + " provider, model, created_at, updated_at)"
                                + " values (:id, :name, :wish, 'draft', 'claude_code', 'anthropic',"
                                + " 'claude-sonnet-5-5', :now, :now)")
                .param("id", id)
                .param("name", nameOf(trimmed))
                .param("wish", trimmed)
                .param("now", now)
                .update();
        return get(id).orElseThrow();
    }

    public List<Project> list() {
        return jdbc.sql("select " + COLUMNS + " from projects.projects order by created_at desc")
                .query(ProjectService::map)
                .list();
    }

    public Optional<Project> get(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from projects.projects where id = :id")
                .param("id", id)
                .query(ProjectService::map)
                .optional();
    }

    @Override
    public Optional<String> wishOf(UUID projectId) {
        return jdbc.sql("select wish from projects.projects where id = :id")
                .param("id", projectId)
                .query(String.class)
                .optional();
    }

    @Override
    public void markStatus(UUID projectId, String status) {
        jdbc.sql(
                        "update projects.projects set status = :status, updated_at = :now,"
                                + " version = version + 1 where id = :id")
                .param("status", status)
                .param("now", Timestamp.from(clock.instant()))
                .param("id", projectId)
                .update();
    }

    private static String nameOf(String wish) {
        String line = wish.lines().findFirst().orElse(wish).strip();
        return line.length() <= NAME_LENGTH ? line : line.substring(0, NAME_LENGTH - 1) + "…";
    }

    private static Project map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new Project(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("wish"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
