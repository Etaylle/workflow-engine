package engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Needs the PostgreSQL of ENGINE_DB_URL; Flyway migrates it when the application starts. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatabaseIT {

    private static final List<String> DOMAIN_SCHEMAS =
            List.of("projects", "questions", "documents", "workflows", "execution");

    @Autowired private JdbcClient jdbc;
    @LocalServerPort private int port;

    @Test
    void migrationsCreateTheTablesOfEveryDomain() {
        Integer tables =
                jdbc.sql(
                                "select count(*) from information_schema.tables"
                                        + " where table_schema in (:schemas)")
                        .param("schemas", DOMAIN_SCHEMAS)
                        .query(Integer.class)
                        .single();

        assertThat(tables).isEqualTo(24);
    }

    @Test
    void noForeignKeyCrossesADomainSchema() {
        Integer crossing =
                jdbc.sql(
                                "select count(*) from information_schema.referential_constraints rc"
                                        + " join information_schema.table_constraints fk"
                                        + "   on fk.constraint_name = rc.constraint_name"
                                        + "  and fk.constraint_schema = rc.constraint_schema"
                                        + " join information_schema.table_constraints pk"
                                        + "   on pk.constraint_name = rc.unique_constraint_name"
                                        + "  and pk.constraint_schema = rc.unique_constraint_schema"
                                        + " where fk.table_schema <> pk.table_schema")
                        .query(Integer.class)
                        .single();

        assertThat(crossing).isZero();
    }

    @Test
    void statusAndHealthAnswer() throws Exception {
        assertThat(get("/api/status")).contains("\"name\":\"workflow-engine\"").contains("\"buildId\":\"");
        assertThat(get("/actuator/health")).contains("\"status\":\"UP\"");
    }

    private String get(String path) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }
}
