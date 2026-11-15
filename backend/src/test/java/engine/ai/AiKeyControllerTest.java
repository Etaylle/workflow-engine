package engine.ai;

import static org.assertj.core.api.Assertions.assertThat;

import engine.shared.SessionKey;
import org.junit.jupiter.api.Test;

class AiKeyControllerTest {

    private static final String KEY = "sk-ant-secret-key-9876";

    private final SessionKey sessionKey = new SessionKey();
    private final AiKeyController controller = new AiKeyController(sessionKey);

    @Test
    void noKeyAtFirst() {
        assertThat(controller.state()).isEqualTo(new AiKeyController.AiKeyState(false, null));
    }

    @Test
    void setKeyShowsOnlyTheLastFourCharacters() {
        AiKeyController.AiKeyState state = controller.replace(new AiKeyController.AiKeyRequest(" " + KEY + " "));

        assertThat(state).isEqualTo(new AiKeyController.AiKeyState(true, "9876"));
        assertThat(sessionKey.get()).contains(KEY);
        assertThat(controller.state().toString()).doesNotContain("secret");
    }

    @Test
    void replacingKeepsOnlyTheNewKey() {
        controller.replace(new AiKeyController.AiKeyRequest(KEY));
        controller.replace(new AiKeyController.AiKeyRequest("sk-ant-other-key-1111"));

        assertThat(sessionKey.get()).contains("sk-ant-other-key-1111");
        assertThat(controller.state().last4()).isEqualTo("1111");
    }

    @Test
    void removingClearsTheKey() {
        controller.replace(new AiKeyController.AiKeyRequest(KEY));

        controller.remove();

        assertThat(sessionKey.get()).isEmpty();
        assertThat(controller.state().set()).isFalse();
    }

    @Test
    void shortKeyShowsNoSuffix() {
        assertThat(controller.replace(new AiKeyController.AiKeyRequest("sk-test")).last4()).isEmpty();
    }

    @Test
    void keyDoesNotAppearInToString() {
        assertThat(new AiKeyController.AiKeyRequest(KEY).toString()).doesNotContain("secret");
    }
}
