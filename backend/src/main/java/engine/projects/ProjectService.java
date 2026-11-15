package engine.projects;

import engine.shared.ApiProblem;
import engine.shared.Harness;
import engine.shared.TemplateCatalog;
import engine.shared.ProjectCleanup;
import engine.shared.AiTarget;
import engine.shared.ProjectPort;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Creates and reads projects; also the answer of the domain to {@link ProjectPort}. */
@Service
public class ProjectService implements ProjectPort {

    static final int MAX_WISH_LENGTH = 20_000;
    static final int MAX_NAME_LENGTH = 120;
    private static final int NAME_LENGTH = 60;
    private static final String COLUMNS = "id, name, wish, status, agent_harness, template_id, build_mode, model, created_at, updated_at,"
            + " (select b.software_kind from projects.project_briefs b where b.project_id = projects.projects.id)"
            + " as brief_kind";

    private final JdbcClient jdbc;
    private final Clock clock;
    private final JsonMapper json;
    private final List<ProjectCleanup> cleanups;
    private final TemplateCatalog templates;

    public ProjectService(JdbcClient jdbc, Clock clock, JsonMapper json, List<ProjectCleanup> cleanups,
            TemplateCatalog templates) {
        this.templates = templates;
        this.cleanups = cleanups;
        this.jdbc = jdbc;
        this.clock = clock;
        this.json = json;
    }

    /** Creates a draft project from the wish; without a name it is the start of the wish. */
    public Project create(String wish, String name) {
        String trimmed = checkedWish(wish);
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.sql(
                        "insert into projects.projects (id, name, wish, status, agent_harness,"
                                + " provider, model, created_at, updated_at)"
                                + " values (:id, :name, :wish, 'draft', 'claude_code', 'anthropic',"
                                + " 'claude-sonnet-5-5', :now, :now)")
                .param("id", id)
                .param("name", name == null || name.isBlank() ? nameOf(trimmed) : checkedName(name))
                .param("wish", trimmed)
                .param("now", now)
                .update();
        return get(id).orElseThrow();
    }

    /**
     * Changes the name and/or the wish. A changed wish moves the project back to draft, so the
     * brief and the questions are made again from the new wish.
     */
    @Transactional
    public Project update(UUID id, String name, String wish, String agentHarness, String templateId,
            String buildMode) {
        if (name == null && wish == null && agentHarness == null && templateId == null
                && buildMode == null) {
            throw new ApiProblem(400, "Nothing to change",
                    "Send a name, a wish, a builder (agentHarness), a template (templateId), a build mode (buildMode), or several.");
        }
        Project current = get(id).orElseThrow(() -> notFound(id));
        String newName = name == null ? current.name() : checkedName(name);
        String newWish = wish == null ? current.wish() : checkedWish(wish);
        boolean wishChanged = !newWish.equals(current.wish());
        String harness = agentHarness == null ? current.agentHarness() : Harness.ofId(agentHarness).id();
        String template = templateId == null ? storedTemplateOf(id) : templates.require(templateId).id();
        String mode = buildMode == null ? current.buildMode() : checkedBuildMode(buildMode);
        jdbc.sql("update projects.projects set name = :name, template_id = :template, build_mode = :mode, wish = :wish, status = :status,"
                        + " agent_harness = :harness, updated_at = :now, version = version + 1 where id = :id")
                .param("harness", harness)
                .param("template", template)
                .param("mode", mode)
                .param("name", newName)
                .param("wish", newWish)
                .param("status", wishChanged ? "draft" : current.status())
                .param("now", Timestamp.from(clock.instant()))
                .param("id", id)
                .update();
        return get(id).orElseThrow();
    }

    /** The build mode of the project: whole, tasks, or auto (stored as null: the contract decides). */
    private static String checkedBuildMode(String buildMode) {
        if ("auto".equals(buildMode)) {
            return null;
        }
        if (!List.of("whole", "tasks").contains(buildMode)) {
            throw new ApiProblem(400, "Unknown build mode", "Choose whole, tasks or auto.");
        }
        return buildMode;
    }

    private static String checkedWish(String wish) {
        String trimmed = wish == null ? "" : wish.strip();
        if (trimmed.isEmpty()) {
            throw new ApiProblem(
                    400, "Wish missing", "Describe what you want built; the wish is empty.");
        }
        if (trimmed.length() > MAX_WISH_LENGTH) {
            throw new ApiProblem(
                    400, "Wish too long", "The wish has more than " + MAX_WISH_LENGTH + " characters.");
        }
        return trimmed;
    }

    private static String checkedName(String name) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiProblem(400, "Name invalid",
                    "The name must have 1 to " + MAX_NAME_LENGTH + " characters.");
        }
        return trimmed;
    }

    /** Deletes the project with everything the domains hold for it. */
    @Transactional
    public void delete(UUID id) {
        if (get(id).isEmpty()) {
            throw notFound(id);
        }
        cleanups.forEach(cleanup -> cleanup.deleteAllOf(id));
        jdbc.sql("delete from projects.projects where id = :id").param("id", id).update();
    }

    static ApiProblem notFound(UUID id) {
        return new ApiProblem(404, "Project not found", "No project " + id);
    }

    public List<Project> list() {
        return jdbc.sql("select " + COLUMNS + " from projects.projects order by created_at desc")
                .query(this::map)
                .list();
    }

    public Optional<Project> get(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from projects.projects where id = :id")
                .param("id", id)
                .query(this::map)
                .optional();
    }

    @Override
    public Optional<String> nameOf(UUID projectId) {
        return jdbc.sql("select name from projects.projects where id = :id")
                .param("id", projectId)
                .query(String.class)
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
    public Optional<String> statusOf(UUID projectId) {
        return jdbc.sql("select status from projects.projects where id = :id")
                .param("id", projectId)
                .query(String.class)
                .optional();
    }

    private String storedTemplateOf(UUID projectId) {
        return jdbc.sql("select template_id from projects.projects where id = :id")
                .param("id", projectId).query((rs, n) -> rs.getString(1)).optional().orElse(null);
    }

    @Override
    public Optional<String> buildModeOf(UUID projectId) {
        return jdbc.sql("select build_mode from projects.projects where id = :id")
                .param("id", projectId)
                .query((rs, n) -> rs.getString(1))
                .optional();
    }

    @Override
    public Optional<String> templateIdOf(UUID projectId) {
        return get(projectId).map(Project::templateId);
    }

    @Override
    public Optional<Harness> harnessOf(UUID projectId) {
        return jdbc.sql("select agent_harness from projects.projects where id = :id")
                .param("id", projectId)
                .query(String.class)
                .optional()
                .map(Harness::ofId);
    }

    @Override
    public Optional<String> briefOf(UUID projectId) {
        return briefRecord(projectId).map(Brief::toText);
    }

    /** The stored brief of the project, or empty when none was written. */
    Optional<Brief> briefRecord(UUID projectId) {
        TypeReference<List<String>> strings = new TypeReference<>() { };
        return jdbc.sql("select goal, actors::text, domain_terms::text, risks::text, software_kind"
                        + " from projects.project_briefs where project_id = :id")
                .param("id", projectId)
                .query((rs, row) -> new Brief(rs.getString(1), json.readValue(rs.getString(2), strings),
                        json.readValue(rs.getString(3), strings), json.readValue(rs.getString(4), strings),
                        rs.getString(5)))
                .optional();
    }

    @Override
    public void recordAi(UUID projectId, AiTarget target) {
        jdbc.sql("update projects.projects set provider = :provider, model = :model where id = :id")
                .param("provider", target.provider().id())
                .param("model", target.model())
                .param("id", projectId)
                .update();
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

    private Project map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        String chosen = rs.getString("template_id");
        String template = chosen != null ? chosen : templates.defaultFor(rs.getString("brief_kind"));
        return new Project(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("wish"),
                rs.getString("status"),
                rs.getString("agent_harness"),
                template,
                rs.getString("build_mode"),
                rs.getString("model"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
