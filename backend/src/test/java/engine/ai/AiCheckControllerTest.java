package engine.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.ApiProblem;
import engine.shared.SessionKey;
import org.junit.jupiter.api.Test;

class AiCheckControllerTest {

    private final SessionKey sessionKey = new SessionKey();
    private final AiCheckController controller =
            new AiCheckController(new StubAiConnection(), sessionKey);

    private static AiCheckController.AiCheckRequest anthropic(String key) {
        return new AiCheckController.AiCheckRequest(null, key, null, null);
    }

    @Test
    void acceptedKeyIsKeptForTheSession() {
        AiCheckController.AiCheckResult result = controller.check(anthropic(" sk-test "));

        assertThat(result.ok()).isTrue();
        assertThat(sessionKey.anthropicKey()).contains("sk-test");
    }

    @Test
    void rejectedKeyIsAnsweredAs401AndKeepsTheSavedKey() {
        sessionKey.setAnthropic("old");

        assertThatThrownBy(() -> controller.check(anthropic("bad")))
                .isInstanceOfSatisfying(
                        AiCheckException.class, e -> assertThat(e.status()).isEqualTo(401));
        assertThat(sessionKey.anthropicKey()).contains("old");
    }

    @Test
    void compatibleKeyIsKeptWithItsBaseUrlAndModel() {
        controller.check(new AiCheckController.AiCheckRequest(
                "openai", "AQ.key", "https://generativelanguage.googleapis.com/v1beta/openai/",
                "gemini-flash-lite-latest"));

        var saved = sessionKey.compatible().orElseThrow();
        assertThat(saved.baseUrl()).isEqualTo("https://generativelanguage.googleapis.com/v1beta/openai/");
        assertThat(saved.model()).isEqualTo("gemini-flash-lite-latest");
        assertThat(sessionKey.anthropicKey()).isEmpty();
    }

    @Test
    void emptyKeyTestsTheSavedKeyAndWithoutOneIsRefused() {
        assertThatThrownBy(() -> controller.check(anthropic("")))
                .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(400));
        sessionKey.setAnthropic("sk-saved");

        assertThat(controller.check(anthropic("")).ok()).isTrue();
    }

    @Test
    void insecureBaseUrlIsRefused() {
        assertThatThrownBy(() -> controller.check(new AiCheckController.AiCheckRequest(
                "openai", "k", "http://example.com/v1", null)))
                .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(400));
    }

    @Test
    void keyDoesNotAppearInToString() {
        assertThat(anthropic("sk-secret").toString()).doesNotContain("sk-secret");
    }
}
