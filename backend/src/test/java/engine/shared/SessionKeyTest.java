package engine.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The keys file: what is saved, who may read it, and what a restart finds. */
class SessionKeyTest {

    private static final String CLAUDE_KEY = "sk-ant-unit-test-key-0001";
    private static final String OTHER_KEY = "sk-unit-test-compatible-key-0002";

    @TempDir Path dir;

    private Path file() {
        return dir.resolve("engine/keys.json");
    }

    @Test
    void keysProviderChoiceAndEndpointAreThereAgainAfterARestart() {
        SessionKey first = new SessionKey(file());
        first.setAnthropic(CLAUDE_KEY);
        first.setCompatible(OTHER_KEY, "https://api.example.com/v1", "model-x");
        first.choose(AiProvider.OPENAI);

        SessionKey restarted = new SessionKey(file());

        assertThat(restarted.anthropicKey()).contains(CLAUDE_KEY);
        assertThat(restarted.compatible()).contains(
                new SessionKey.Compatible(OTHER_KEY, "https://api.example.com/v1", "model-x"));
        assertThat(restarted.active()).contains(AiProvider.OPENAI);
        assertThat(restarted.saved()).isTrue();
    }

    @Test
    void onlyTheOwnerMayReadOrWriteTheFile() throws IOException {
        new SessionKey(file()).setAnthropic(CLAUDE_KEY);

        assertThat(Files.getPosixFilePermissions(file()))
                .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        assertThat(Files.list(file().getParent())).as("no half-written copy is left").hasSize(1);
    }

    @Test
    void aRemovedKeyIsGoneFromTheFileAndTheFileGoesWithTheLastKey() throws IOException {
        SessionKey keys = new SessionKey(file());
        keys.setAnthropic(CLAUDE_KEY);
        keys.setCompatible(OTHER_KEY, null, null);

        keys.clear(AiProvider.ANTHROPIC);

        assertThat(Files.readString(file())).doesNotContain(CLAUDE_KEY).contains(OTHER_KEY);
        assertThat(new SessionKey(file()).anthropicKey()).isEmpty();

        keys.clear(AiProvider.OPENAI);

        assertThat(file()).doesNotExist();
    }

    @Test
    void withoutAKeysFileNothingIsWrittenAndARestartForgetsTheKey() throws IOException {
        SessionKey memoryOnly = new SessionKey();
        memoryOnly.setAnthropic(CLAUDE_KEY);

        assertThat(memoryOnly.saved()).isFalse();
        assertThat(Files.list(dir)).isEmpty();
        assertThat(new SessionKey().anthropicKey()).isEmpty();
    }

    @Test
    void aFileTheEngineDidNotWriteStopsTheStartAndSaysWhatToDo() throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "not json");

        assertThatThrownBy(() -> new SessionKey(file())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(file().toString()).hasMessageContaining("Delete the file");
    }

    @Test
    void theKeyNeverShowsInTheTextOfTheCompatibleProvider() {
        SessionKey keys = new SessionKey(file());
        keys.setCompatible(OTHER_KEY, null, null);

        assertThat(keys.compatible().orElseThrow().toString()).doesNotContain(OTHER_KEY);
        assertThat(Set.of(keys.compatible().orElseThrow().baseUrl())).containsExactly("https://api.openai.com/v1");
    }
}
