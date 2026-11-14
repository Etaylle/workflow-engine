package engine.questions;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Tables of the schema "questions": the questions of a project and the answers to them. */
@Repository
class QuestionStore {

    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() { };

    private final JdbcClient jdbc;
    private final Clock clock;
    private final JsonMapper json;

    QuestionStore(JdbcClient jdbc, Clock clock, JsonMapper json) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.json = json;
    }

    /** Replaces the questions of the project (and with them the answers) by the draft. */
    @Transactional
    void replace(UUID projectId, QuestionDraft draft) {
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.sql("delete from questions.questions where project_id = :p").param("p", projectId).update();
        int seq = 1;
        for (QuestionDraft.Item item : draft.questions()) {
            insert(projectId, seq++, item, now);
        }
    }

    private void insert(UUID projectId, int seq, QuestionDraft.Item item, Timestamp now) {
        jdbc.sql("insert into questions.questions (id, project_id, seq, category, answer_type, text,"
                        + " options, ai_default, explanation, created_at, updated_at) values (:id, :p,"
                        + " :seq, :category, :type, :text, cast(:options as jsonb), :proposal,"
                        + " :explanation, :now, :now)")
                .param("id", UUID.randomUUID()).param("p", projectId).param("seq", seq)
                .param("category", item.category()).param("type", item.answerType())
                .param("text", item.text().strip())
                .param("options", json.writeValueAsString(item.options()))
                .param("proposal", proposalJson(item))
                .param("explanation", item.explanation()).param("now", now).update();
    }

    private String proposalJson(QuestionDraft.Item item) {
        JsonNode proposal = "multiple_choice".equals(item.answerType())
                ? json.valueToTree(item.proposal())
                : json.valueToTree(item.proposal().get(0).strip());
        return json.writeValueAsString(proposal);
    }

    /** Appends a question at the end. */
    void add(UUID projectId, QuestionDraft.Item item) {
        int next = jdbc.sql("select coalesce(max(seq), 0) + 1 from questions.questions"
                + " where project_id = :p").param("p", projectId).query(Integer.class).single();
        insert(projectId, next, item, Timestamp.from(clock.instant()));
    }

    /** Rewrites one question and drops its answer; false when the project has no such question. */
    @Transactional
    boolean update(UUID projectId, UUID questionId, QuestionDraft.Item item) {
        int rows = jdbc.sql("update questions.questions set category = :category,"
                        + " answer_type = :type, text = :text, options = cast(:options as jsonb),"
                        + " ai_default = :proposal, explanation = :explanation, updated_at = :now"
                        + " where id = :id and project_id = :p")
                .param("category", item.category()).param("type", item.answerType())
                .param("text", item.text().strip())
                .param("options", json.writeValueAsString(item.options()))
                .param("proposal", proposalJson(item)).param("explanation", item.explanation())
                .param("now", Timestamp.from(clock.instant())).param("id", questionId)
                .param("p", projectId).update();
        jdbc.sql("delete from questions.answers where question_id = :id").param("id", questionId).update();
        return rows > 0;
    }

    /** Removes one question with its answer and closes the gap in the numbering. */
    @Transactional
    boolean delete(UUID projectId, UUID questionId) {
        Integer seq = jdbc.sql("select seq from questions.questions where id = :id and project_id = :p")
                .param("id", questionId).param("p", projectId).query(Integer.class).optional().orElse(null);
        if (seq == null) {
            return false;
        }
        jdbc.sql("delete from questions.questions where id = :id").param("id", questionId).update();
        jdbc.sql("update questions.questions set seq = -seq where project_id = :p and seq > :s")
                .param("p", projectId).param("s", seq).update();
        jdbc.sql("update questions.questions set seq = -seq - 1 where project_id = :p and seq < 0")
                .param("p", projectId).update();
        return true;
    }

    /** The questions of the project in order, each with its answer when one exists. */
    List<QuestionView> find(UUID projectId) {
        return jdbc.sql("select q.id, q.seq, q.category, q.answer_type, q.text, q.options::text,"
                        + " q.ai_default, q.explanation, a.value::text as value, a.decided_by"
                        + " from questions.questions q left join questions.answers a"
                        + " on a.question_id = q.id where q.project_id = :p order by q.seq")
                .param("p", projectId)
                .query((rs, row) -> new QuestionView(rs.getObject("id", UUID.class), rs.getInt("seq"),
                        rs.getString("category"), rs.getString("answer_type"), rs.getString("text"),
                        json.readValue(rs.getString("options"), STRINGS),
                        json.readTree(rs.getString("ai_default")), rs.getString("explanation"),
                        rs.getString("value") == null ? null
                                : new QuestionView.Answer(json.readTree(rs.getString("value")),
                                        rs.getString("decided_by"))))
                .list();
    }

    /** Replaces all answers of the project by the given ones (question id to answer). */
    @Transactional
    void saveAnswers(UUID projectId, Map<UUID, QuestionView.Answer> answers) {
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.sql("delete from questions.answers where question_id in"
                        + " (select id from questions.questions where project_id = :p)")
                .param("p", projectId).update();
        answers.forEach((questionId, answer) -> jdbc.sql(
                        "insert into questions.answers (id, question_id, value, decided_by, created_at,"
                                + " updated_at) values (:id, :q, cast(:value as jsonb), :by, :now, :now)")
                .param("id", UUID.randomUUID()).param("q", questionId)
                .param("value", json.writeValueAsString(answer.value()))
                .param("by", answer.decidedBy()).param("now", now).update());
    }
}
