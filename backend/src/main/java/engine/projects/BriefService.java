package engine.projects;

import engine.shared.ApiProblem;
import engine.shared.AiTarget;
import engine.shared.PlanningCall;
import engine.shared.SessionKey;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Writes (one planning call, one retry) and reads the brief of a project. */
@Service
class BriefService {

    private final BriefWriter writer;
    private final ProjectService projects;
    private final SessionKey sessionKey;
    private final JdbcClient jdbc;
    private final Clock clock;
    private final JsonMapper json;

    BriefService(BriefWriter writer, ProjectService projects, SessionKey sessionKey,
            JdbcClient jdbc, Clock clock, JsonMapper json) {
        this.writer = writer;
        this.projects = projects;
        this.sessionKey = sessionKey;
        this.jdbc = jdbc;
        this.clock = clock;
        this.json = json;
    }

    /** Writes the brief while the project is a draft; writing it again replaces it. */
    Brief generate(UUID projectId) {
        Project project = projects.get(projectId).orElseThrow(() -> notFound(projectId));
        if (!"draft".equals(project.status())) {
            throw new ApiProblem(409, "Brief is fixed", "The brief can only be written while the"
                    + " project is a draft; the questions are already based on it.");
        }
        AiTarget target = PlanningCall.requireTarget(sessionKey);
        projects.recordAi(projectId, target);
        Brief brief = PlanningCall.validated("brief",
                correction -> writer.write(target, project.wish(), correction), Brief::validate);
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.sql("insert into projects.project_briefs (project_id, goal, actors, domain_terms, risks,"
                        + " software_kind, created_at, updated_at) values (:p, :goal, cast(:actors as jsonb),"
                        + " cast(:terms as jsonb), cast(:risks as jsonb), :kind, :now, :now)"
                        + " on conflict (project_id) do update set goal = :goal, software_kind = :kind,"
                        + " actors = cast(:actors as jsonb), domain_terms = cast(:terms as jsonb),"
                        + " risks = cast(:risks as jsonb), updated_at = :now")
                .param("p", projectId).param("goal", brief.goal().strip())
                .param("actors", json.writeValueAsString(brief.actors()))
                .param("terms", json.writeValueAsString(brief.domainTerms()))
                .param("risks", json.writeValueAsString(brief.risks()))
                .param("kind", brief.softwareKind()).param("now", now).update();
        projects.proposeTemplate(projectId, brief.softwareKind());
        return find(projectId).orElseThrow();
    }

    Optional<Brief> find(UUID projectId) {
        projects.get(projectId).orElseThrow(() -> notFound(projectId));
        return projects.briefRecord(projectId);
    }

    private static ApiProblem notFound(UUID id) {
        return new ApiProblem(404, "Project not found", "No project " + id);
    }
}
