package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import engine.shared.Template;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The use check with small real programs as ./start.sh; no browser is started here. */
class UseCheckTest {

    @TempDir
    Path workspace;
    @TempDir
    Path logs;

    private final List<String> console = new ArrayList<>();
    /** Short waits, and no browser script: the browser part is then skipped and says so. */
    private final UseCheck check = new UseCheck(true, 20, 1, "no-such-browser-script.cjs", "none");
    private Template free;

    @BeforeEach
    void template() {
        free = new Template("free", "Free", Template.ANY, "", false, workspace);
    }

    @Test
    void aConsoleProgramThatPrintsAndEndsPasses() throws IOException {
        declare("console");
        start("echo 'quack quack'");

        UseCheck.Result result = run();

        assertThat(result.passed()).isTrue();
        assertThat(result.lines()).anyMatch(line -> line.contains("ended with exit code 0"))
                .contains("quack quack");
        assertThat(console).anyMatch(line -> line.startsWith("use: "));
    }

    @Test
    void aConsoleProgramThatWaitsForAKeyPassesWhenItHasPrinted() throws IOException {
        declare("console");
        start("echo 'press a key'; read -r _key; echo never");

        UseCheck.Result result = run();

        assertThat(result.passed()).isTrue();
        assertThat(result.lines()).anyMatch(line -> line.contains("waits for input or keeps running"));
    }

    @Test
    void aConsoleProgramThatCrashesFailsWithItsOutput() throws IOException {
        declare("console");
        start("echo 'TypeError: duck is not defined' >&2; exit 3");

        UseCheck.Result result = run();

        assertThat(result.passed()).isFalse();
        assertThat(result.lines()).anyMatch(line -> line.contains("exit code 3"))
                .contains("TypeError: duck is not defined");
    }

    @Test
    void aConsoleProgramThatPrintsNothingFails() throws IOException {
        declare("console");
        start("exit 0");

        UseCheck.Result result = run();

        assertThat(result.passed()).isFalse();
        assertThat(result.lines()).anyMatch(line -> line.contains("without printing anything"));
    }

    @Test
    void aMissingStartScriptFails() {
        UseCheck.Result result = run();

        assertThat(result.passed()).isFalse();
        assertThat(result.lines()).anyMatch(line -> line.contains("./start.sh is missing"));
    }

    @Test
    void aWebAppThatAnswersOnThePortOfTheEnginePasses() throws IOException {
        declare("web");
        start(server("response.end('<h1>Ferret Invaders</h1>')"));

        UseCheck.Result result = run();

        assertThat(result.passed()).isTrue();
        assertThat(result.lines()).anyMatch(line -> line.contains("browser check skipped"));
    }

    @Test
    void aWebAppWhoseStartPageFailsIsRefused() throws IOException {
        declare("web");
        start(server("response.statusCode = 500; response.end('boom')"));

        UseCheck.Result result = run();

        assertThat(result.passed()).isFalse();
        assertThat(result.lines()).anyMatch(line -> line.contains("answered HTTP 500"));
    }

    @Test
    void aWebAppThatStopsBeforeItAnswersFailsWithItsOutput() throws IOException {
        declare("web");
        start("echo 'Error: Cannot find module vite' >&2; exit 1");

        UseCheck.Result result = run();

        assertThat(result.passed()).isFalse();
        assertThat(result.lines()).anyMatch(line -> line.contains("before the app answered"))
                .contains("Error: Cannot find module vite");
    }

    private UseCheck.Result run() {
        return check.run(free, workspace, Map.of(), logs, () -> false, console::add);
    }

    private void declare(String kind) throws IOException {
        Files.writeString(workspace.resolve("app.json"), "{\"kind\": \"" + kind + "\"}");
    }

    private void start(String body) throws IOException {
        Path script = workspace.resolve("start.sh");
        Files.writeString(script, "#!/usr/bin/env bash\n" + body + "\n");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private static String server(String handler) {
        return "exec node -e \"require('http').createServer((request, response) => { " + handler
                + " }).listen(process.env.APP_PORT)\"";
    }
}
