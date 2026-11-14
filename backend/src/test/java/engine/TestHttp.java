package engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Small HTTP helper of the integration tests. */
public final class TestHttp {

    public static final JsonMapper JSON = JsonMapper.builder().build();

    private final int port;

    public TestHttp(int port) {
        this.port = port;
    }

    public HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public JsonNode json(String method, String path, String body) throws Exception {
        return JSON.readTree(call(method, path, body).body());
    }

    public UUID createProject(String wish) throws Exception {
        String body = JSON.writeValueAsString(Map.of("wish", wish, "apiKey", "sk-test"));
        HttpResponse<String> response = call("POST", "/api/projects", body);
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(JSON.readTree(response.body()).get("id").asString());
    }

    public String statusOf(UUID project) throws Exception {
        return json("GET", "/api/projects/" + project, null).get("status").asString();
    }
}
