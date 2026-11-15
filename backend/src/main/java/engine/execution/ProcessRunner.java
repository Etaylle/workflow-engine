package engine.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Runs one child process with a bounded wait; output goes to files, never into memory or logs. */
final class ProcessRunner {

    /** Exit code, or -1 when the time ran out or the run was stopped (the process tree was killed). */
    record Result(int exitCode, boolean timedOut, boolean cancelled) {
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
        return run(command, dir, env, stdout, stderr, timeout, () -> false);
    }

    /** As above; the process is killed within a second after {@code cancelled} turns true. */
    static Result run(List<String> command, Path dir, Map<String, String> env, Path stdout,
            Path stderr, Duration timeout, BooleanSupplier cancelled) {
        ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile())
                .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")))
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
        builder.environment().putAll(env);
        try {
            Process process = builder.start();
            long deadline = System.nanoTime() + timeout.toNanos();
            while (!process.waitFor(1, TimeUnit.SECONDS)) {
                boolean stop = cancelled.getAsBoolean();
                if (stop || System.nanoTime() > deadline) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                    return new Result(-1, !stop, stop);
                }
            }
            return new Result(process.exitValue(), false, false);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start " + command.get(0)
                    + "; is it installed and on the PATH?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + command.get(0), e);
        }
    }
}
