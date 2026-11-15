package engine.settings;

import engine.shared.RunSettingsPort.Step;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Tables of the schema settings. */
@Repository
class SettingsStore {

    /** The columns a project may have chosen; null means the default. */
    record Chosen(UUID profileId, BigDecimal costLimitUsd) {
    }

    private final JdbcClient jdbc;
    private final Clock clock;

    SettingsStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    private Timestamp now() {
        return Timestamp.from(clock.instant());
    }

    List<StepProfileView> profiles() {
        return jdbc.sql("select id from settings.step_profiles order by seeded desc, is_default desc, name")
                .query(UUID.class).list().stream().map(id -> profile(id).orElseThrow()).toList();
    }

    Optional<StepProfileView> profile(UUID id) {
        return jdbc.sql("select id, name, description, is_default, seeded from settings.step_profiles"
                        + " where id = :id").param("id", id)
                .query((rs, n) -> new StepProfileView(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("description"), rs.getBoolean("is_default"), rs.getBoolean("seeded"),
                        steps(id)))
                .optional();
    }

    Optional<UUID> defaultProfileId() {
        return jdbc.sql("select id from settings.step_profiles where is_default").query(UUID.class)
                .optional();
    }

    private List<Step> steps(UUID profileId) {
        return jdbc.sql("select key, title, gated, cap_usd, prompt from settings.profile_steps"
                        + " where profile_id = :p order by seq").param("p", profileId)
                .query((rs, n) -> new Step(rs.getString("key"), rs.getString("title"),
                        rs.getBoolean("gated"), rs.getBigDecimal("cap_usd"), rs.getString("prompt")))
                .list();
    }

    boolean nameTaken(String name, UUID exceptId) {
        return jdbc.sql("select count(*) from settings.step_profiles where lower(name) = lower(:n)"
                        + " and id <> :id").param("n", name).param("id", exceptId)
                .query(Integer.class).single() > 0;
    }

    /** Copies the profile and its steps under a new name; the copy is the client's own. */
    @Transactional
    UUID copy(UUID fromId, String name, String description) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into settings.step_profiles (id, name, description, created_at, updated_at)"
                        + " values (:id, :name, :d, :now, :now)")
                .param("id", id).param("name", name).param("d", description).param("now", now()).update();
        replaceSteps(id, steps(fromId));
        return id;
    }

    @Transactional
    void update(UUID id, String name, List<Step> steps) {
        jdbc.sql("update settings.step_profiles set name = :n, updated_at = :now where id = :id")
                .param("n", name).param("now", now()).param("id", id).update();
        replaceSteps(id, steps);
    }

    private void replaceSteps(UUID profileId, List<Step> steps) {
        jdbc.sql("delete from settings.profile_steps where profile_id = :p").param("p", profileId).update();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            jdbc.sql("insert into settings.profile_steps (id, profile_id, seq, key, title, gated, cap_usd,"
                            + " prompt) values (:id, :p, :seq, :key, :title, :gated, :cap, :prompt)")
                    .param("id", UUID.randomUUID()).param("p", profileId).param("seq", i + 1)
                    .param("key", step.key()).param("title", step.title()).param("gated", step.gated())
                    .param("cap", step.capUsd()).param("prompt", step.prompt()).update();
        }
    }

    void delete(UUID id) {
        jdbc.sql("delete from settings.step_profiles where id = :id").param("id", id).update();
    }

    Chosen chosen(UUID projectId) {
        return jdbc.sql("select step_profile_id, cost_limit_usd from settings.project_settings"
                        + " where project_id = :p").param("p", projectId)
                .query((rs, n) -> new Chosen(rs.getObject("step_profile_id", UUID.class),
                        rs.getBigDecimal("cost_limit_usd")))
                .optional().orElse(new Chosen(null, null));
    }

    void choose(UUID projectId, Chosen chosen) {
        jdbc.sql("insert into settings.project_settings (project_id, step_profile_id, cost_limit_usd,"
                        + " updated_at) values (:p, :profile, :limit, :now) on conflict (project_id)"
                        + " do update set step_profile_id = :profile, cost_limit_usd = :limit,"
                        + " updated_at = :now")
                .param("p", projectId).param("profile", chosen.profileId())
                .param("limit", chosen.costLimitUsd()).param("now", now()).update();
    }

    void deleteAllOf(UUID projectId) {
        jdbc.sql("delete from settings.project_settings where project_id = :p").param("p", projectId)
                .update();
    }
}
