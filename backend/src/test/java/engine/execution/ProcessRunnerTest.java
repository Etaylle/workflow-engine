package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProcessRunnerTest {

    @TempDir Path dir;

    @Test
    void linesAreHandedOnWhileTheProcessStillRuns() throws IOException {
        List<String> lines = new CopyOnWriteArrayList<>();

        ProcessRunner.Result result = ProcessRunner.run(
                List.of("sh", "-c", "echo first; sleep 2; echo second; printf last"), dir, Map.of(),
                dir.resolve("out"), dir.resolve("err"), Duration.ofSeconds(20), () -> false, lines::add);

        assertThat(result.exitCode()).isZero();
        assertThat(lines).containsExactly("first", "second", "last");
        assertThat(Files.readString(dir.resolve("out"))).startsWith("first");
    }

    @Test
    void aProcessThatRunsTooLongIsKilledAndItsLinesAreKept() {
        List<String> lines = new CopyOnWriteArrayList<>();

        ProcessRunner.Result result = ProcessRunner.run(List.of("sh", "-c", "echo hi; sleep 30"), dir,
                Map.of(), dir.resolve("out"), dir.resolve("err"), Duration.ofSeconds(1), () -> false,
                lines::add);

        assertThat(result.timedOut()).isTrue();
        assertThat(lines).containsExactly("hi");
    }
}
