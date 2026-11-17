package engine.settings;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.TestHttp;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

/** Step profiles and project settings through HTTP and the real PostgreSQL. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedAi.class)
class SettingsIT {

    @Autowired private JdbcClient jdbc;
    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void setUp() {
        http = new TestHttp(port);
    }

    @AfterEach
    void removeCopies() {
        jdbc.sql("delete from settings.step_profiles where key is null").update();
    }

    private JsonNode profileNamed(String name) throws Exception {
        for (JsonNode profile : http.json("GET", "/api/settings/step-profiles", null)) {
            if (profile.get("name").asString().equals(name)) {
                return profile;
            }
        }
        throw new AssertionError("No profile " + name);
    }

    @Test
    void twoProfilesAreSeededAndTheShortOneIsTheDefault() throws Exception {
        JsonNode profiles = http.json("GET", "/api/settings/step-profiles", null);

        JsonNode shortProfile = profileNamed("Short");
        JsonNode full = profileNamed("16 steps");
        assertThat(profiles.size()).isGreaterThanOrEqualTo(2);
        assertThat(shortProfile.get("isDefault").asBoolean()).isTrue();
        assertThat(shortProfile.get("steps")).extracting(s -> s.get("key").asString())
                .containsExactly("plan", "plan-review", "build", "ci", "check");
        assertThat(full.get("isDefault").asBoolean()).isFalse();
        assertThat(full.get("seeded").asBoolean()).isTrue();
        assertThat(full.get("steps")).hasSize(16);
        assertThat(full.get("steps").get(5).get("title").asString()).isEqualTo("Build");
        assertThat(full.get("steps").get(5).get("gated").asBoolean()).isTrue();
        for (JsonNode step : full.get("steps")) {
            assertThat(step.get("prompt").asString()).doesNotContain("Order a meal");
        }
        assertThat(shortProfile.get("steps").get(0).get("prompt").asString()).contains("{task}");
    }

    @Test
    void theIndependentCheckComparesWithTheOriginalWishAndTheKindOfSoftware() throws Exception {
        for (String name : new String[] {"Short", "16 steps"}) {
            JsonNode steps = profileNamed(name).get("steps");
            String prompt = steps.get(steps.size() - 1).get("prompt").asString();

            assertThat(prompt).contains("ORIGINAL WISH").contains("Software template")
                    .contains("not the kind of software the wish asks for").contains("./start.sh")
                    .contains("README.md");
            assertThat(prompt.split("ORIGINAL WISH", -1)).as("added once, not twice").hasSize(2);
        }
    }

    @Test
    void theSeedGivesTheSameResultWhenRunTwice() throws Exception {
        String seed = new ClassPathResource("db/migration/R__seed_step_profiles.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        jdbc.sql("delete from settings.step_profiles where key is null").update();
        long profiles = count("settings.step_profiles");
        long steps = count("settings.profile_steps");

        jdbc.sql(seed).update();
        jdbc.sql(seed).update();

        assertThat(count("settings.step_profiles")).isEqualTo(profiles);
        assertThat(count("settings.profile_steps")).isEqualTo(steps);
    }

    private long count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    @Test
    void aSeededProfileIsReadOnlyButACopyCanBeEditedReorderedAndDeleted() throws Exception {
        JsonNode shortProfile = profileNamed("Short");
        String id = shortProfile.get("id").asString();
        assertThat(http.call("PUT", "/api/settings/step-profiles/" + id,
                "{\"name\":\"x\",\"steps\":[]}").statusCode()).isEqualTo(409);
        assertThat(http.call("DELETE", "/api/settings/step-profiles/" + id, null).statusCode())
                .isEqualTo(409);

        JsonNode copy = http.json("POST", "/api/settings/step-profiles",
                "{\"fromId\":\"" + id + "\",\"name\":\"Mine\"}");
        assertThat(copy.get("seeded").asBoolean()).isFalse();
        assertThat(copy.get("steps")).hasSize(5);
        String copyId = copy.get("id").asString();
        String edited = "{\"name\":\"Mine 2\",\"steps\":[{\"key\":\"build\",\"title\":\"Build it\","
                + "\"gated\":true,\"capUsd\":2.5,\"prompt\":\"Build {task}\"},"
                + "{\"key\":\"plan\",\"title\":\"Plan\",\"gated\":false,\"capUsd\":0.5,"
                + "\"prompt\":\"Plan {task}\"}]}";

        JsonNode saved = http.json("PUT", "/api/settings/step-profiles/" + copyId, edited);

        assertThat(saved.get("name").asString()).isEqualTo("Mine 2");
        assertThat(saved.get("steps")).extracting(s -> s.get("key").asString())
                .containsExactly("build", "plan");
        assertThat(saved.get("steps").get(0).get("capUsd").asDouble()).isEqualTo(2.5);
        assertThat(profileNamed("Short").get("steps")).hasSize(5);
        assertThat(http.call("DELETE", "/api/settings/step-profiles/" + copyId, null).statusCode())
                .isEqualTo(204);
        assertThat(http.call("DELETE", "/api/settings/step-profiles/" + copyId, null).statusCode())
                .isEqualTo(404);
    }

    @Test
    void badCopiesAndBadEditsAreRefusedWithTheRuleThatWasBroken() throws Exception {
        String id = profileNamed("Short").get("id").asString();
        assertThat(http.call("POST", "/api/settings/step-profiles",
                "{\"fromId\":\"" + id + "\",\"name\":\" \"}").statusCode()).isEqualTo(400);
        assertThat(http.call("POST", "/api/settings/step-profiles",
                "{\"fromId\":\"" + id + "\",\"name\":\"short\"}").statusCode()).isEqualTo(400);
        assertThat(http.call("POST", "/api/settings/step-profiles", "{\"fromId\":\""
                + UUID.randomUUID() + "\",\"name\":\"Z\"}").statusCode()).isEqualTo(404);
        String copyId = http.json("POST", "/api/settings/step-profiles",
                "{\"fromId\":\"" + id + "\",\"name\":\"Mine\"}").get("id").asString();
        String step = "{\"key\":\"a\",\"title\":\"A\",\"gated\":false,\"capUsd\":1,\"prompt\":\"p\"}";

        assertThat(put(copyId, "[]")).contains("1 to 20 steps");
        assertThat(put(copyId, "[" + step + "," + step + "]")).contains("used twice");
        assertThat(put(copyId, "[" + step.replace("\"a\"", "\"A b\"") + "]")).contains("key");
        assertThat(put(copyId, "[" + step.replace("\"capUsd\":1", "\"capUsd\":11") + "]"))
                .contains("cost cap");
        assertThat(put(copyId, "[" + step.replace("\"capUsd\":1", "\"capUsd\":0") + "]"))
                .contains("cost cap");
        assertThat(put(copyId, "[" + step.replace("\"p\"", "\"\"") + "]")).contains("prompt");
        assertThat(put(copyId, "[" + step.replace("\"A\"", "\" \"") + "]")).contains("title");
        String only = step.replace("\"gated\"", "\"reviewerOnly\":true,\"gated\"");
        assertThat(put(copyId, "[" + only + "]")).contains("only an independent review step");
        assertThat(put(copyId, "[" + only.replace("\"gated\"", "\"review\":true,\"gated\"") + "]"))
                .contains("at least one step that always runs");
        assertThat(profileNamed("Mine").get("steps")).hasSize(5);
    }

    private String put(String id, String steps) throws Exception {
        var response = http.call("PUT", "/api/settings/step-profiles/" + id,
                "{\"name\":\"Mine\",\"steps\":" + steps + "}");
        assertThat(response.statusCode()).isEqualTo(400);
        return response.body();
    }

    @Test
    void aProjectChoosesAProfileAndALimitAndFallsBackToTheDefaults() throws Exception {
        UUID project = http.createProject("A canteen app");
        JsonNode initial = http.json("GET", "/api/projects/" + project + "/settings", null);
        assertThat(initial.get("stepProfileName").asString()).isEqualTo("Short");
        assertThat(initial.get("usesDefaultProfile").asBoolean()).isTrue();
        assertThat(initial.get("usesDefaultLimit").asBoolean()).isTrue();
        assertThat(initial.get("costLimitUsd").asDouble()).isEqualTo(10.0);
        String fullId = profileNamed("16 steps").get("id").asString();

        JsonNode chosen = http.json("PUT", "/api/projects/" + project + "/settings",
                "{\"stepProfileId\":\"" + fullId + "\",\"costLimitUsd\":25.5}");

        assertThat(chosen.get("stepProfileName").asString()).isEqualTo("16 steps");
        assertThat(chosen.get("usesDefaultProfile").asBoolean()).isFalse();
        assertThat(chosen.get("costLimitUsd").asDouble()).isEqualTo(25.5);
        assertThat(http.json("GET", "/api/projects/" + project + "/settings", null)
                .get("usesDefaultLimit").asBoolean()).isFalse();
        JsonNode reset = http.json("PUT", "/api/projects/" + project + "/settings", "{}");
        assertThat(reset.get("usesDefaultProfile").asBoolean()).isTrue();
        assertThat(reset.get("costLimitUsd").asDouble()).isEqualTo(10.0);
    }

    @Test
    void projectSettingsRefuseUnknownThingsAndADeletedProfileFallsBackToTheDefault() throws Exception {
        UUID project = http.createProject("A canteen app");
        String path = "/api/projects/" + project + "/settings";
        assertThat(http.call("PUT", path, "{\"costLimitUsd\":0.5}").statusCode()).isEqualTo(400);
        assertThat(http.call("PUT", path, "{\"costLimitUsd\":1001}").statusCode()).isEqualTo(400);
        assertThat(http.call("PUT", path, "{\"stepProfileId\":\"" + UUID.randomUUID() + "\"}")
                .statusCode()).isEqualTo(404);
        assertThat(http.call("GET", "/api/projects/" + UUID.randomUUID() + "/settings", null)
                .statusCode()).isEqualTo(404);
        String shortId = profileNamed("Short").get("id").asString();
        String copyId = http.json("POST", "/api/settings/step-profiles",
                "{\"fromId\":\"" + shortId + "\",\"name\":\"Mine\"}").get("id").asString();
        http.json("PUT", path, "{\"stepProfileId\":\"" + copyId + "\"}");

        http.call("DELETE", "/api/settings/step-profiles/" + copyId, null);

        assertThat(http.json("GET", path, null).get("stepProfileName").asString()).isEqualTo("Short");
    }

    @Test
    void deletingTheProjectRemovesItsSettings() throws Exception {
        UUID project = http.createProject("A canteen app");
        http.json("PUT", "/api/projects/" + project + "/settings", "{\"costLimitUsd\":12}");

        http.call("DELETE", "/api/projects/" + project, null);

        assertThat(jdbc.sql("select count(*) from settings.project_settings where project_id = :p")
                .param("p", project).query(Long.class).single()).isZero();
    }
}
