package engine.documents;

import engine.shared.RequirementsPort.RequirementLine;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Tables of the schema "documents": the epics and requirements of a project. */
@Repository
class RequirementStore {

    private static final Map<String, String> PREFIX =
            Map.of("functional", "FR", "quality", "QR", "constraint", "CON");

    private final JdbcClient jdbc;
    private final Clock clock;

    RequirementStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Replaces the epics and requirements of the project; the engine assigns the keys. */
    @Transactional
    void replace(UUID projectId, RequirementDraft draft) {
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.sql("delete from documents.requirements where project_id = :p").param("p", projectId).update();
        jdbc.sql("delete from documents.epics where project_id = :p").param("p", projectId).update();
        Map<String, Integer> counters = new HashMap<>();
        int epicSeq = 1;
        for (RequirementDraft.Epic epic : draft.epics()) {
            UUID epicId = UUID.randomUUID();
            jdbc.sql("insert into documents.epics (id, project_id, key, name, domain_name, seq,"
                            + " created_at, updated_at) values (:id, :p, :key, :name, :domain, :seq, :now, :now)")
                    .param("id", epicId).param("p", projectId).param("key", String.format("E-%02d", epicSeq))
                    .param("name", epic.name().strip()).param("domain", epic.domainName().strip())
                    .param("seq", epicSeq++).param("now", now).update();
            for (RequirementDraft.Item item : epic.requirements()) {
                String prefix = PREFIX.get(item.type());
                String key = String.format("%s-%03d", prefix, counters.merge(prefix, 1, Integer::sum));
                jdbc.sql("insert into documents.requirements (id, project_id, epic_id, key, type, text,"
                                + " priority, source, created_at, updated_at) values (:id, :p, :epic, :key,"
                                + " :type, :text, :priority, :source, :now, :now)")
                        .param("id", UUID.randomUUID()).param("p", projectId).param("epic", epicId)
                        .param("key", key).param("type", item.type()).param("text", item.text().strip())
                        .param("priority", item.priority())
                        .param("source", item.source() == null || item.source().isBlank() ? null
                                : item.source().strip())
                        .param("now", now).update();
            }
        }
    }

    RequirementsView find(UUID projectId) {
        List<RequirementsView.Epic> epics = new ArrayList<>();
        for (EpicRow epic : jdbc.sql("select id, key, name, domain_name from documents.epics"
                        + " where project_id = :p order by seq").param("p", projectId)
                .query((rs, row) -> new EpicRow(rs.getObject("id", UUID.class), rs.getString("key"),
                        rs.getString("name"), rs.getString("domain_name"))).list()) {
            List<RequirementsView.Requirement> items = jdbc.sql(
                            "select key, type, priority, text, source from documents.requirements"
                                    + " where epic_id = :e order by created_at, key")
                    .param("e", epic.id())
                    .query((rs, row) -> new RequirementsView.Requirement(rs.getString("key"),
                            rs.getString("type"), rs.getString("priority"), rs.getString("text"),
                            rs.getString("source")))
                    .list();
            epics.add(new RequirementsView.Epic(epic.key(), epic.name(), epic.domain(), items));
        }
        return new RequirementsView(epics);
    }

    List<RequirementLine> lines(UUID projectId) {
        return find(projectId).epics().stream()
                .flatMap(epic -> epic.requirements().stream().map(r -> new RequirementLine(
                        r.key(), r.type(), r.priority(), epic.name(), r.text())))
                .toList();
    }

    private record EpicRow(UUID id, String key, String name, String domain) {
    }
}
