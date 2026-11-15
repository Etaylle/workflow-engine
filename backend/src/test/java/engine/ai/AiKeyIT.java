package engine.ai;

import static org.assertj.core.api.Assertions.assertThat;

import engine.ScriptedAi;
import engine.TestHttp;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/** The key of the session through HTTP: set, replace, remove, and never shown. */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedAi.class)
class AiKeyIT {

    @LocalServerPort private int port;
    private TestHttp http;

    @BeforeEach
    void setUp() throws Exception {
        http = new TestHttp(port);
        http.call("DELETE", "/api/ai/key", null);
    }

    @AfterEach
    void clear() throws Exception {
        http.call("DELETE", "/api/ai/key", null);
    }

    @Test
    void setReplaceAndRemoveTheKeyWithoutEverShowingIt() throws Exception {
        assertThat(http.call("GET", "/api/ai/key", null).body()).contains("\"set\":false");

        HttpResponse<String> set = http.call("PUT", "/api/ai/key", "{\"apiKey\":\"sk-ant-very-secret-1234\"}");
        assertThat(set.statusCode()).isEqualTo(200);
        assertThat(set.body()).contains("\"set\":true", "\"last4\":\"1234\"").doesNotContain("secret");

        http.call("PUT", "/api/ai/key", "{\"apiKey\":\"sk-ant-other-secret-5678\"}");
        String state = http.call("GET", "/api/ai/key", null).body();
        assertThat(state).contains("5678").doesNotContain("secret");

        assertThat(http.call("DELETE", "/api/ai/key", null).statusCode()).isEqualTo(204);
        assertThat(http.call("GET", "/api/ai/key", null).body()).contains("\"set\":false");
    }

    @Test
    void blankKeyIsRejected() throws Exception {
        assertThat(http.call("PUT", "/api/ai/key", "{\"apiKey\":\"  \"}").statusCode()).isEqualTo(400);
    }

    @Test
    void removedKeyMakesPlanningCallsAskForAKey() throws Exception {
        var project = http.createProject("A canteen app");
        http.call("DELETE", "/api/ai/key", null);

        HttpResponse<String> response = http.call("POST", "/api/projects/" + project + "/brief", null);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("Anthropic key missing");
    }
}
