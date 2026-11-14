package engine.execution;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Tables of the schema "execution": runs, tasks, steps, agent calls, checkpoints, events. */
@Repository
class RunStore {

    /** What the orchestrator needs to know about a task it builds. */
    record TaskRow(UUID id, String title, String description, List<StepRow> steps) {
    }

    /** What the orchestrator needs to know about a step it runs. */
    record StepRow(UUID id, String key) {
    }

    /** A run row without tasks. */
    record RunRow(UUID id, UUID projectId, String workspacePath, String databaseName,
            BigDecimal costLimitUsd, int contractVersion) {
    }

    private final JdbcClient jdbc;
    private final Clock clock;

    RunStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    private Timestamp now() {
        return Timestamp.from(clock.instant());
    }

    /** Creates the run with all its tasks and steps waiting (the plan is visible at once). */
    @Transactional
    void create(UUID runId, UUID projectId, int contractVersion, BigDecimal limit, String workspacePath,
            String databaseName, List<engine.shared.ContractPort.TaskLine> tasks, List<StepDef> steps) {
        jdbc.sql("insert into execution.runs (id, project_id, contract_version, status, cost_limit_usd,"
                        + " workspace_path, database_name, started_at) values (:id, :p, :v, 'preparing',"
                        + " :limit, :ws, :db, :now)")
                .param("id", runId).param("p", projectId).param("v", contractVersion)
                .param("limit", limit).param("ws", workspacePath).param("db", databaseName)
                .param("now", now()).update();
        for (int t = 0; t < tasks.size(); t++) {
            UUID taskId = UUID.randomUUID();
            engine.shared.ContractPort.TaskLine task = tasks.get(t);
            jdbc.sql("insert into execution.task_runs (id, run_id, seq, title, lane, description, status)"
                            + " values (:id, :r, :seq, :title, :lane, :d, 'pending')")
                    .param("id", taskId).param("r", runId).param("seq", t + 1)
                    .param("title", task.title()).param("lane", task.lane())
                    .param("d", task.description()).update();
            for (int s = 0; s < steps.size(); s++) {
                jdbc.sql("insert into execution.step_runs (id, task_run_id, seq, step_key, title, status)"
                                + " values (:id, :t, :seq, :key, :title, 'waiting')")
                        .param("id", UUID.randomUUID()).param("t", taskId).param("seq", s + 1)
                        .param("key", steps.get(s).key()).param("title", steps.get(s).title()).update();
            }
        }
    }

    Optional<RunRow> row(UUID runId) {
        return jdbc.sql("select id, project_id, workspace_path, database_name, cost_limit_usd,"
                        + " contract_version from execution.runs where id = :id")
                .param("id", runId)
                .query((rs, n) -> new RunRow(rs.getObject("id", UUID.class),
                        rs.getObject("project_id", UUID.class), rs.getString("workspace_path"),
                        rs.getString("database_name"), rs.getBigDecimal("cost_limit_usd"),
                        rs.getInt("contract_version")))
                .optional();
    }

    List<TaskRow> tasksToBuild(UUID runId) {
        List<TaskRow> tasks = new ArrayList<>();
        jdbc.sql("select id, title, description from execution.task_runs where run_id = :r order by seq")
                .param("r", runId).query((rs, n) -> new Object[] {rs.getObject("id", UUID.class),
                        rs.getString("title"), rs.getString("description")})
                .list().forEach(t -> tasks.add(new TaskRow((UUID) t[0], (String) t[1], (String) t[2],
                        jdbc.sql("select id, step_key from execution.step_runs where task_run_id = :t"
                                        + " order by seq").param("t", t[0])
                                .query((rs, n) -> new StepRow(rs.getObject("id", UUID.class),
                                        rs.getString("step_key")))
                                .list())));
        return tasks;
    }

    boolean projectHasActiveRun(UUID projectId) {
        return jdbc.sql("select count(*) from execution.runs where project_id = :p"
                        + " and status in ('preparing', 'executing')")
                .param("p", projectId).query(Integer.class).single() > 0;
    }

    void setRun(UUID runId, String status, String reason) {
        boolean finished = "done".equals(status) || "paused".equals(status) || "interrupted".equals(status);
        jdbc.sql("update execution.runs set status = :s, failure_reason = :reason, updated_at = :now,"
                        + " finished_at = cast(:fin as timestamptz) where id = :id")
                .param("s", status).param("reason", reason).param("now", now())
                .param("fin", finished ? now() : null).param("id", runId).update();
    }

    void setTask(UUID taskId, String status) {
        jdbc.sql("update execution.task_runs set status = :s where id = :id")
                .param("s", status).param("id", taskId).update();
    }

    void setStep(UUID stepId, String status, String result) {
        jdbc.sql("update execution.step_runs set status = :s, result = :r where id = :id")
                .param("s", status).param("r", result).param("id", stepId).update();
    }

    void addAgentCall(UUID stepId, int attempt, StepOutcome outcome) {
        jdbc.sql("insert into execution.agent_calls (id, step_run_id, attempt, seconds, cost_usd, turns,"
                        + " outcome, created_at) values (:id, :s, :a, :sec, :cost, :turns, :o, :now)")
                .param("id", UUID.randomUUID()).param("s", stepId).param("a", attempt)
                .param("sec", outcome.seconds()).param("cost", outcome.costUsd())
                .param("turns", outcome.turns()).param("o", outcome.subtype()).param("now", now())
                .update();
    }

    void addCheckpoint(UUID stepId, String sha) {
        jdbc.sql("insert into execution.checkpoints (id, step_run_id, commit_sha, created_at)"
                        + " values (:id, :s, :sha, :now)")
                .param("id", UUID.randomUUID()).param("s", stepId).param("sha", sha)
                .param("now", now()).update();
    }

    BigDecimal costOf(UUID runId) {
        return jdbc.sql("select coalesce(sum(c.cost_usd), 0) from execution.agent_calls c"
                        + " join execution.step_runs s on s.id = c.step_run_id"
                        + " join execution.task_runs t on t.id = s.task_run_id where t.run_id = :r")
                .param("r", runId).query(BigDecimal.class).single();
    }

    /** Appends an event with the next number of the run (the update makes the number atomic). */
    RunEvent append(UUID runId, String kind, String text) {
        Timestamp at = now();
        long seq = jdbc.sql("update execution.runs set last_seq = last_seq + 1 where id = :id"
                        + " returning last_seq").param("id", runId).query(Long.class).single();
        jdbc.sql("insert into execution.run_events (run_id, seq, kind, text, created_at)"
                        + " values (:r, :seq, :k, :t, :at)")
                .param("r", runId).param("seq", seq).param("k", kind).param("t", text)
                .param("at", at).update();
        return new RunEvent(seq, kind, text, at.toInstant());
    }

    List<RunEvent> events(UUID runId, long after, int limit) {
        return jdbc.sql("select seq, kind, text, created_at from execution.run_events"
                        + " where run_id = :r and seq > :after order by seq limit :limit")
                .param("r", runId).param("after", after).param("limit", limit)
                .query((rs, n) -> new RunEvent(rs.getLong("seq"), rs.getString("kind"),
                        rs.getString("text"), rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    Optional<UUID> latestRunId(UUID projectId) {
        return jdbc.sql("select id from execution.runs where project_id = :p"
                        + " order by started_at desc limit 1")
                .param("p", projectId).query(UUID.class).optional();
    }

    Optional<RunView> view(UUID runId) {
        Optional<RunView> head = jdbc.sql("select id, project_id, contract_version, status, failure_reason,"
                        + " cost_limit_usd, last_seq, started_at, finished_at from execution.runs"
                        + " where id = :id").param("id", runId)
                .query((rs, n) -> new RunView(runId, rs.getObject("project_id", UUID.class),
                        rs.getInt("contract_version"), rs.getString("status"),
                        rs.getString("failure_reason"), BigDecimal.ZERO,
                        rs.getBigDecimal("cost_limit_usd"), rs.getLong("last_seq"),
                        rs.getTimestamp("started_at").toInstant(),
                        rs.getTimestamp("finished_at") == null ? null
                                : rs.getTimestamp("finished_at").toInstant(),
                        List.of()))
                .optional();
        return head.map(run -> new RunView(run.id(), run.projectId(), run.contractVersion(), run.status(),
                run.failureReason(), costOf(runId), run.costLimitUsd(), run.lastSeq(), run.startedAt(),
                run.finishedAt(), tasksOf(runId)));
    }

    private List<RunView.Task> tasksOf(UUID runId) {
        return jdbc.sql("select id, seq, title, lane, status from execution.task_runs"
                        + " where run_id = :r order by seq").param("r", runId)
                .query((rs, n) -> new RunView.Task(rs.getObject("id", UUID.class), rs.getInt("seq"),
                        rs.getString("title"), rs.getString("lane"), rs.getString("status"),
                        stepsOf(rs.getObject("id", UUID.class))))
                .list();
    }

    private List<RunView.Step> stepsOf(UUID taskId) {
        return jdbc.sql("select s.id, s.seq, s.step_key, s.title, s.status, s.result,"
                        + " coalesce((select sum(cost_usd) from execution.agent_calls where step_run_id = s.id), 0) cost,"
                        + " coalesce((select sum(turns) from execution.agent_calls where step_run_id = s.id), 0) turns,"
                        + " coalesce((select sum(seconds) from execution.agent_calls where step_run_id = s.id), 0) seconds,"
                        + " (select commit_sha from execution.checkpoints where step_run_id = s.id"
                        + "  order by created_at desc limit 1) sha"
                        + " from execution.step_runs s where s.task_run_id = :t order by s.seq")
                .param("t", taskId)
                .query((rs, n) -> new RunView.Step(rs.getObject("id", UUID.class), rs.getInt("seq"),
                        rs.getString("step_key"), rs.getString("title"), rs.getString("status"),
                        rs.getString("result"), rs.getBigDecimal("cost"), rs.getInt("turns"),
                        rs.getInt("seconds"), rs.getString("sha")))
                .list();
    }

    /** Marks runs that were under way when the engine stopped; returns their ids. */
    @Transactional
    List<UUID> interruptActive() {
        List<UUID> ids = jdbc.sql("select id from execution.runs where status in ('preparing', 'executing')")
                .query(UUID.class).list();
        for (UUID id : ids) {
            jdbc.sql("update execution.step_runs set status = 'failed', result = 'Interrupted: the engine"
                            + " restarted' where status = 'running' and task_run_id in"
                            + " (select id from execution.task_runs where run_id = :r)")
                    .param("r", id).update();
            jdbc.sql("update execution.task_runs set status = 'failed' where run_id = :r"
                    + " and status = 'running'").param("r", id).update();
            setRun(id, "interrupted", "The engine restarted while the run was under way; the key is gone.");
        }
        return ids;
    }

    List<RunRow> runsOf(UUID projectId) {
        return jdbc.sql("select id from execution.runs where project_id = :p").param("p", projectId)
                .query(UUID.class).list().stream().map(id -> row(id).orElseThrow()).toList();
    }

    void deleteAllOf(UUID projectId) {
        jdbc.sql("delete from execution.runs where project_id = :p").param("p", projectId).update();
    }
}
