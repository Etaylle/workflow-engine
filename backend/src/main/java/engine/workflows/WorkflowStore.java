package engine.workflows;

import engine.shared.AnswersPort;
import engine.shared.RequirementsPort;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Tables of the schema "workflows": the workflow of a project and its contract versions. */
@Repository
class WorkflowStore {

    private final JdbcClient jdbc;
    private final Clock clock;

    WorkflowStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Replaces the workflow of the project by the draft (one per project; version counts up). */
    @Transactional
    WorkflowView replace(UUID projectId, WorkflowDraft draft) {
        Timestamp now = Timestamp.from(clock.instant());
        Optional<UUID> existing = idOf(projectId);
        UUID workflowId = existing.orElseGet(UUID::randomUUID);
        if (existing.isPresent()) {
            jdbc.sql("delete from workflows.lanes where workflow_id = :id").param("id", workflowId).update();
            jdbc.sql("update workflows.workflows set status = 'valid', version = version + 1,"
                            + " updated_at = :now where id = :id")
                    .param("now", now).param("id", workflowId).update();
        } else {
            jdbc.sql("insert into workflows.workflows (id, project_id, status, created_at, updated_at)"
                            + " values (:id, :project, 'valid', :now, :now)")
                    .param("id", workflowId).param("project", projectId).param("now", now).update();
        }
        insertContent(workflowId, draft, now);
        return find(projectId).orElseThrow();
    }

    private void insertContent(UUID workflowId, WorkflowDraft draft, Timestamp now) {
        Map<String, UUID> laneIds = new HashMap<>();
        for (int seq = 0; seq < draft.lanes().size(); seq++) {
            UUID id = UUID.randomUUID();
            laneIds.put(draft.lanes().get(seq), id);
            jdbc.sql("insert into workflows.lanes (id, workflow_id, name, seq, created_at, updated_at)"
                            + " values (:id, :wf, :name, :seq, :now, :now)")
                    .param("id", id).param("wf", workflowId).param("name", draft.lanes().get(seq))
                    .param("seq", seq).param("now", now).update();
        }
        Map<String, int[]> positions = WorkflowLayout.positions(draft);
        Map<String, UUID> nodeIds = new HashMap<>();
        for (WorkflowDraft.Node node : draft.nodes()) {
            UUID id = UUID.randomUUID();
            nodeIds.put(node.key(), id);
            boolean task = "task".equals(node.kind());
            jdbc.sql("insert into workflows.nodes (id, workflow_id, lane_id, kind, title, description,"
                            + " task_type, pos_x, pos_y, created_at, updated_at) values (:id, :wf, :lane,"
                            + " :kind, :title, :description, :type, :x, :y, :now, :now)")
                    .param("id", id).param("wf", workflowId).param("lane", laneIds.get(node.lane()))
                    .param("kind", node.kind()).param("title", node.title().strip())
                    .param("description", node.description() == null || node.description().isBlank()
                            ? null : node.description().strip())
                    .param("type", task ? node.taskType() : null)
                    .param("x", positions.get(node.key())[0]).param("y", positions.get(node.key())[1])
                    .param("now", now).update();
        }
        for (WorkflowDraft.Edge edge : draft.edges()) {
            jdbc.sql("insert into workflows.edges (id, workflow_id, source_node_id, target_node_id,"
                            + " created_at) values (:id, :wf, :source, :target, :now)")
                    .param("id", UUID.randomUUID()).param("wf", workflowId)
                    .param("source", nodeIds.get(edge.from())).param("target", nodeIds.get(edge.to()))
                    .param("now", now).update();
        }
    }

    private Optional<UUID> idOf(UUID projectId) {
        return jdbc.sql("select id from workflows.workflows where project_id = :p")
                .param("p", projectId).query(UUID.class).optional();
    }

    Optional<WorkflowView> find(UUID projectId) {
        return jdbc.sql("select id, status, version from workflows.workflows where project_id = :p")
                .param("p", projectId)
                .query((rs, row) -> content(projectId, rs.getObject("id", UUID.class),
                        rs.getString("status"), rs.getInt("version")))
                .optional();
    }

    private WorkflowView content(UUID projectId, UUID id, String status, int version) {
        List<WorkflowView.Lane> lanes = jdbc.sql(
                        "select id, name from workflows.lanes where workflow_id = :id order by seq")
                .param("id", id)
                .query((rs, row) -> new WorkflowView.Lane(rs.getObject("id", UUID.class), rs.getString("name")))
                .list();
        List<WorkflowView.Node> nodes = jdbc.sql(
                        "select id, lane_id, kind, title, description, task_type, pos_x, pos_y"
                                + " from workflows.nodes where workflow_id = :id order by pos_x, pos_y, id")
                .param("id", id)
                .query((rs, row) -> new WorkflowView.Node(rs.getObject("id", UUID.class),
                        rs.getObject("lane_id", UUID.class), rs.getString("kind"), rs.getString("title"),
                        rs.getString("description"), rs.getString("task_type"), rs.getInt("pos_x"),
                        rs.getInt("pos_y")))
                .list();
        List<WorkflowView.Edge> edges = jdbc.sql(
                        "select id, source_node_id, target_node_id from workflows.edges"
                                + " where workflow_id = :id order by created_at, id")
                .param("id", id)
                .query((rs, row) -> new WorkflowView.Edge(rs.getObject("id", UUID.class),
                        rs.getObject("source_node_id", UUID.class), rs.getObject("target_node_id", UUID.class)))
                .list();
        Integer contract = jdbc.sql("select max(contract_version) from workflows.contracts where project_id = :p")
                .param("p", projectId).query(Integer.class).optional().orElse(null);
        return new WorkflowView(id, projectId, status, version, contract, lanes, nodes, edges);
    }

    /** Frozen copy of everything the contract rests on: wish, brief, answers, requirements, workflow. */
    record Snapshot(String wish, String brief, List<AnswersPort.AnswerLine> answers,
            List<RequirementsPort.RequirementLine> requirements, WorkflowView workflow) {
    }

    /**
     * Confirms the valid workflow as the next contract version.
     *
     * @return the new contract, or empty when the workflow is missing or not in status valid
     */
    @Transactional
    Optional<ContractView> confirm(UUID projectId, String snapshotJson) {
        Optional<String> status = jdbc.sql(
                        "select status from workflows.workflows where project_id = :p for update")
                .param("p", projectId).query(String.class).optional();
        if (status.isEmpty() || !"valid".equals(status.get())) {
            return Optional.empty();
        }
        Timestamp now = Timestamp.from(clock.instant());
        int version = jdbc.sql("select coalesce(max(contract_version), 0) + 1 from workflows.contracts"
                        + " where project_id = :p").param("p", projectId).query(Integer.class).single();
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into workflows.contracts (id, project_id, contract_version, snapshot,"
                        + " confirmed_at) values (:id, :p, :v, cast(:s as jsonb), :now)")
                .param("id", id).param("p", projectId).param("v", version).param("s", snapshotJson)
                .param("now", now).update();
        jdbc.sql("update workflows.workflows set status = 'confirmed', version = version + 1,"
                        + " updated_at = :now where project_id = :p").param("now", now).param("p", projectId).update();
        return Optional.of(new ContractView(id, projectId, version, now.toInstant()));
    }

    /** The newest contract version with its frozen snapshot as JSON text. */
    Optional<StoredContract> latestContract(UUID projectId) {
        return jdbc.sql("select contract_version, snapshot::text as snapshot, cost_limit_amount"
                        + " from workflows.contracts where project_id = :p"
                        + " order by contract_version desc limit 1")
                .param("p", projectId)
                .query((rs, row) -> new StoredContract(rs.getInt("contract_version"),
                        rs.getString("snapshot"), rs.getBigDecimal("cost_limit_amount")))
                .optional();
    }

    /** A contract row: version, snapshot JSON and the limit (null when none was chosen). */
    record StoredContract(int version, String snapshot, java.math.BigDecimal costLimit) {
    }

    List<ContractView> contracts(UUID projectId) {
        return jdbc.sql("select id, contract_version, confirmed_at from workflows.contracts"
                        + " where project_id = :p order by contract_version")
                .param("p", projectId)
                .query((rs, row) -> new ContractView(rs.getObject("id", UUID.class), projectId,
                        rs.getInt("contract_version"), rs.getTimestamp("confirmed_at").toInstant()))
                .list();
    }
}
