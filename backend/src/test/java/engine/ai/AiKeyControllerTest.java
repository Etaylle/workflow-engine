package engine.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.ApiProblem;
import engine.shared.SessionKey;
import org.junit.jupiter.api.Test;

class AiKeyControllerTest {

    private static final String KEY = "sk-ant-secret-key-9876";

    private final SessionKey sessionKey = new SessionKey();
    private final AiKeyController controller = new AiKeyController(sessionKey);

    private static AiKeyController.SetKeyRequest key(String apiKey) {
        return new AiKeyController.SetKeyRequest(apiKey, null, null);
    }

    @Test
    void noKeyAtFirstAndDefaultsForThePanel() {
        var state = controller.state();

        assertThat(state.anthropic().set()).isFalse();
        assertThat(state.openai().set()).isFalse();
        assertThat(state.planning()).isNull();
        assertThat(state.defaults().baseUrl()).isEqualTo("https://api.openai.com/v1");
    }

    @Test
    void setKeyShowsOnlyTheLastFourCharacters() {
        var state = controller.replace("anthropic", key(" " + KEY + " "));

        assertThat(state.anthropic().last4()).isEqualTo("9876");
        assertThat(sessionKey.anthropicKey()).contains(KEY);
        assertThat(state.toString()).doesNotContain("secret");
        assertThat(state.planning().provider()).isEqualTo("anthropic");
    }

    @Test
    void replacingKeepsOnlyTheNewKey() {
        controller.replace("anthropic", key(KEY));
        controller.replace("anthropic", key("sk-ant-other-key-1111"));

        assertThat(sessionKey.anthropicKey()).contains("sk-ant-other-key-1111");
    }

    @Test
    void compatibleKeyKeepsBaseUrlAndModelAndNeverShowsTheKey() {
        var state = controller.replace("openai", new AiKeyController.SetKeyRequest(
                "AQ.very-secret-key-4321", "https://api.groq.com/openai/v1", "llama-x"));

        assertThat(state.openai().last4()).isEqualTo("4321");
        assertThat(state.openai().baseUrl()).isEqualTo("https://api.groq.com/openai/v1");
        assertThat(state.openai().model()).isEqualTo("llama-x");
        assertThat(state.toString()).doesNotContain("secret");
        assertThat(state.planning().provider()).isEqualTo("openai");
    }

    @Test
    void bothKeysSetPlanningFollowsTheChoice() {
        controller.replace("anthropic", key(KEY));
        controller.replace("openai", key("AQ.other-key-0000"));
        assertThat(controller.state().planning().provider()).isEqualTo("anthropic");

        var chosen = controller.choose(new AiKeyController.ChooseRequest("openai"));

        assertThat(chosen.planning().provider()).isEqualTo("openai");
        controller.remove("openai");
        assertThat(controller.state().planning().provider()).isEqualTo("anthropic");
    }

    @Test
    void removingClearsOnlyThatKey() {
        controller.replace("anthropic", key(KEY));
        controller.replace("openai", key("AQ.other-key-0000"));

        controller.remove("anthropic");

        assertThat(sessionKey.anthropicKey()).isEmpty();
        assertThat(sessionKey.compatible()).isPresent();
    }

    @Test
    void shortKeyShowsNoSuffix() {
        assertThat(controller.replace("anthropic", key("sk-test")).anthropic().last4()).isEmpty();
    }

    @Test
    void unknownProviderAndInsecureUrlAreRefused() {
        assertThatThrownBy(() -> controller.replace("other", key("k")))
                .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(400));
        assertThatThrownBy(() -> controller.replace("openai",
                new AiKeyController.SetKeyRequest("k", "http://example.com/v1", null)))
                .isInstanceOfSatisfying(ApiProblem.class, e -> assertThat(e.status()).isEqualTo(400));
        assertThat(controller.replace("openai",
                new AiKeyController.SetKeyRequest("k", "http://localhost:11434/v1", null))
                .openai().baseUrl()).isEqualTo("http://localhost:11434/v1");
    }

    @Test
    void keyDoesNotAppearInToString() {
        assertThat(key(KEY).toString()).doesNotContain("secret");
    }
}
