package engine.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Runs one child process with a bounded wait; output goes to files, never into memory or logs. */
final class ProcessRunner {

    /** Exit code, or -1 when the time ran out (the process tree was killed). */
    record Result(int exitCode, boolean timedOut) {
    }

    private ProcessRunner() {
    }

    /**
     * Runs the command in the folder.
     *
     * @param env variables added to the environment of the child only
     * @param stdout file that receives standard output
     * @param stderr file that receives standard error
     */
    static Result run(List<String> command, Path dir, Map<String, String> env, Path stdout,
            Path stderr, Duration timeout) {
        ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile())
                .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")))
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
        builder.environment().putAll(env);
        try {
            Process process = builder.start();
            if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                return new Result(-1, true);
            }
            return new Result(process.exitValue(), false);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start " + command.get(0)
                    + "; is it installed and on the PATH?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + command.get(0), e);
        }
    }
}
