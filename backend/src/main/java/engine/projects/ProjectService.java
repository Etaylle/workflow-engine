package engine.projects;

import engine.shared.ApiProblem;
import engine.shared.BuilderChoice;
import engine.shared.Harness;
import engine.shared.TemplateCatalog;
import engine.shared.ProjectCleanup;
import engine.shared.AiTarget;
import engine.shared.ProjectPort;
import engine.shared.ReviewerChoice;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
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
    private static final String COLUMNS = "id, name, wish, status, agent_harness, template_id, build_mode, model, builder_model, builder_effort, reviewer_harness, reviewer_model, reviewer_effort, created_at, updated_at,"
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
            String buildMode, String builderModel, String builderEffort, ReviewerChange reviewer) {
        if (name == null && wish == null && agentHarness == null && templateId == null
                && buildMode == null && builderModel == null && builderEffort == null && reviewer.isEmpty()) {
            throw new ApiProblem(400, "Nothing to change",
                    "Send a name, a wish, a builder (agentHarness), a template (templateId), a build mode (buildMode), a builder model (builderModel), a builder effort (builderEffort), a reviewer (reviewerHarness, reviewerModel, reviewerEffort), or several.");
        }
        Project current = get(id).orElseThrow(() -> notFound(id));
        String newName = name == null ? current.name() : checkedName(name);
        String newWish = wish == null ? current.wish() : checkedWish(wish);
        boolean wishChanged = !newWish.equals(current.wish());
        String harness = agentHarness == null ? current.agentHarness() : Harness.ofId(agentHarness).id();
        BuilderChoice choice = nextChoice(current, harness, builderModel, builderEffort);
        Reviewer nextReviewer = nextReviewer(current, harness, choice, reviewer);
        String template = templateId == null ? storedTemplateOf(id) : templates.require(templateId).id();
        String mode = buildMode == null ? current.buildMode() : checkedBuildMode(buildMode);
        jdbc.sql("update projects.projects set name = :name, template_id = :template, build_mode = :mode, builder_model = :bmodel, builder_effort = :beffort, reviewer_harness = :rharness, reviewer_model = :rmodel,"
                        + " reviewer_effort = :reffort, wish = :wish, status = :status,"
                        + " agent_harness = :harness, updated_at = :now, version = version + 1 where id = :id")
                .param("harness", harness)
                .param("template", template)
                .param("mode", mode)
                .param("bmodel", choice.model())
                .param("beffort", choice.effort())
                .param("rharness", nextReviewer.harness())
                .param("rmodel", nextReviewer.model())
                .param("reffort", nextReviewer.effort())
                .param("name", newName)
                .param("wish", newWish)
                .param("status", wishChanged ? "draft" : current.status())
                .param("now", Timestamp.from(clock.instant()))
                .param("id", id)
                .update();
        return get(id).orElseThrow();
    }

    /** The reviewer fields of a PATCH; null = not sent, blank harness = no separate reviewer. */
    public record ReviewerChange(String harness, String model, String effort) {

        boolean isEmpty() {
            return harness == null && model == null && effort == null;
        }
    }

    private record Reviewer(String harness, String model, String effort) {
    }

    /**
     * The reviewer after a change: a blank harness removes it, a new harness clears its model and
     * effort, a sent value replaces the old one. It must differ from the builder in harness or model.
     */
    private static Reviewer nextReviewer(Project current, String builderHarness, BuilderChoice builder,
            ReviewerChange change) {
        String harness = change.harness() == null ? current.reviewerHarness() : blankToNull(change.harness());
        if (harness == null) {
            if (change.harness() == null && (change.model() != null || change.effort() != null)) {
                throw new ApiProblem(400, "No reviewer chosen",
                        "Send reviewerHarness before a reviewer model or effort.");
            }
            return new Reviewer(null, null, null);
        }
        Harness reviewerHarness = Harness.ofId(harness);
        boolean same = reviewerHarness.id().equals(current.reviewerHarness());
        BuilderChoice choice = BuilderChoice.checked(reviewerHarness,
                change.model() == null && same ? current.reviewerModel() : change.model(),
                change.effort() == null && same ? current.reviewerEffort() : change.effort());
        if (reviewerHarness.id().equals(builderHarness) && Objects.equals(choice.model(), builder.model())) {
            throw new ApiProblem(400, "Reviewer must differ from the builder",
                    "The reviewer uses the same builder and model as the one that writes the software."
                            + " Choose another builder or another model for the reviewer.");
        }
        return new Reviewer(reviewerHarness.id(), choice.model(), choice.effort());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * The builder's model and effort after a change: a new builder clears both, a sent value (blank =
     * the default) replaces the old one, and the result is checked for the builder.
     */
    private static BuilderChoice nextChoice(Project current, String harness, String model, String effort) {
        boolean sameBuilder = harness.equals(current.agentHarness());
        String keptModel = sameBuilder ? current.builderModel() : null;
        String keptEffort = sameBuilder ? current.builderEffort() : null;
        return BuilderChoice.checked(Harness.ofId(harness), model == null ? keptModel : model,
                effort == null ? keptEffort : effort);
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
    public Optional<BuilderChoice> builderChoiceOf(UUID projectId) {
        return get(projectId).map(p -> new BuilderChoice(p.builderModel(), p.builderEffort()));
    }

    @Override
    public Optional<ReviewerChoice> reviewerOf(UUID projectId) {
        return get(projectId).filter(p -> p.reviewerHarness() != null).map(p -> {
            Harness harness = Harness.ofId(p.reviewerHarness());
            return new ReviewerChoice(harness, new BuilderChoice(p.reviewerModel(), p.reviewerEffort()));
        });
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
                rs.getString("builder_model"),
                rs.getString("builder_effort"),
                rs.getString("reviewer_harness"),
                rs.getString("reviewer_model"),
                rs.getString("reviewer_effort"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
