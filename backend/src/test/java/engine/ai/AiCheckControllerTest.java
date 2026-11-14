package engine.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.SessionKey;
import org.junit.jupiter.api.Test;

class AiCheckControllerTest {

    private final SessionKey sessionKey = new SessionKey();
    private final AiCheckController controller =
            new AiCheckController(new StubAiConnection(), sessionKey);

    @Test
    void acceptedKeyIsKeptForTheSession() {
        AiCheckController.AiCheckResult result =
                controller.check(new AiCheckController.AiCheckRequest(" sk-test "));

        assertThat(result.ok()).isTrue();
        assertThat(sessionKey.get()).contains("sk-test");
    }

    @Test
    void rejectedKeyIsDiscardedAndAnsweredAs401() {
        sessionKey.set("old");

        assertThatThrownBy(() -> controller.check(new AiCheckController.AiCheckRequest("bad")))
                .isInstanceOfSatisfying(
                        AiCheckException.class, e -> assertThat(e.status()).isEqualTo(401));
        assertThat(sessionKey.get()).isEmpty();
    }

    @Test
    void keyDoesNotAppearInToString() {
        assertThat(new AiCheckController.AiCheckRequest("sk-secret").toString())
                .doesNotContain("sk-secret");
    }
}
