package engine.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Planning through Claude Code and its sign-in, against a fake claude program. */
class ClaudeCliCallTest {

    /** A small planning answer. */
    public record Tiny(String name, List<String> tags) {
    }

    private static final String FAKE = """
            #!/usr/bin/env bash
            set -euo pipefail
            here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
            printf '%s\\n' "$*" >"$here/args.log"
            cat >"$here/stdin.log"
            printf 'key=%s cwd=%s\\n' "${ANTHROPIC_API_KEY:-none}" "$PWD" >"$here/env.log"
            prev=""
            for arg in "$@"; do
            	if [ "$prev" = "--system-prompt-file" ]; then cat "$arg" >"$here/system.log"; fi
            	prev="$arg"
            done
            cat "$here/answer.json"
            exit "$(cat "$here/exit")"
            """;

    @TempDir Path bin;
    private AiTarget target;

    @BeforeEach
    void setUp() throws IOException {
        Path fake = Files.writeString(bin.resolve("claude"), FAKE);
        assertThat(fake.toFile().setExecutable(true)).isTrue();
        target = AiTarget.claudeSignIn(fake.toString(), "fable");
        answers("{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":false,\"result\":\"x\","
                + "\"structured_output\":{\"name\":\"Ferret Hunt\",\"tags\":[\"game\",\"ducks\"]}}", 0);
    }

    private void answers(String json, int exitCode) throws IOException {
        Files.writeString(bin.resolve("answer.json"), json);
        Files.writeString(bin.resolve("exit"), String.valueOf(exitCode));
    }

    private Tiny call() {
        return ClaudeCliCall.structured(target, "You plan software.", "Wish: shoot ducks", Tiny.class);
    }

    @Test
    void theStructuredAnswerOfTheCliIsReadAsTheClass() {
        assertThat(call()).isEqualTo(new Tiny("Ferret Hunt", List.of("game", "ducks")));
    }

    @Test
    void theCliGetsTheMaterialTheInstructionsTheModelAndTheSchemaAndNoToolsOrKey() throws IOException {
        call();

        assertThat(Files.readString(bin.resolve("stdin.log"))).isEqualTo("Wish: shoot ducks");
        assertThat(Files.readString(bin.resolve("system.log"))).isEqualTo("You plan software.");
        assertThat(Files.readString(bin.resolve("args.log"))).contains("-p ", "--model fable",
                "--output-format json", "--json-schema {", "\"name\"", "\"tags\"", "--tools  ",
                "--no-session-persistence");
        assertThat(Files.readString(bin.resolve("args.log")))
                .as("the CLI refuses a schema that names its dialect").doesNotContain("$schema");
        String env = Files.readString(bin.resolve("env.log"));
        assertThat(env).as("the sign-in is used, never a key of the engine process").contains("key=none");
        Path folder = Path.of(env.strip().substring(env.indexOf("cwd=") + 4));
        assertThat(folder).as("the folder of the call is removed with its material").doesNotExist();
    }

    @Test
    void aCliThatIsNotSignedInSaysHowToSignInOrEnterAKey() throws IOException {
        answers("{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":true,"
                + "\"result\":\"Not logged in · Please run /login\"}", 1);

        assertThatThrownBy(this::call).isInstanceOf(ApiProblem.class)
                .hasMessageContaining("not signed in").hasMessageContaining("log in")
                .hasMessageContaining("key panel")
                .satisfies(e -> assertThat(((ApiProblem) e).status()).isEqualTo(409));
    }

    @Test
    void aUsageLimitIsToldAsALimitWithWhatTheCliSaid() throws IOException {
        answers("{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":true,"
                + "\"result\":\"You have hit your usage limit, resets 3am\"}", 1);

        assertThatThrownBy(this::call).isInstanceOf(ApiProblem.class).hasMessageContaining("resets 3am")
                .satisfies(e -> assertThat(((ApiProblem) e).status()).isEqualTo(429));
    }

    @Test
    void anAnswerWithoutStructuredOutputIsInvalidSoThatTheCallIsTriedAgain() throws IOException {
        answers("{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":false,\"result\":\"hello\"}", 0);

        assertThatThrownBy(this::call).isInstanceOf(InvalidAiOutputException.class);
    }

    @Test
    void outputThatIsNotJsonIsAFailedCallWithTheExitCodeAndTheText() throws IOException {
        answers("segmentation fault", 139);

        assertThatThrownBy(this::call).isInstanceOf(ApiProblem.class).hasMessageContaining("139")
                .hasMessageContaining("segmentation fault");
    }

    @Test
    void aMissingCommandSaysToInstallClaudeCodeOrEnterAKey() {
        AiTarget gone = AiTarget.claudeSignIn(bin.resolve("no-such-claude").toString(), "fable");

        assertThatThrownBy(() -> ClaudeCliCall.structured(gone, "s", "u", Tiny.class))
                .isInstanceOf(ApiProblem.class).hasMessageContaining("did not start")
                .hasMessageContaining("key panel");
    }
}
