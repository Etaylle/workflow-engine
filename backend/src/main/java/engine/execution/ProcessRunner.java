package engine.execution;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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
        return run(command, dir, env, stdout, stderr, timeout, cancelled, line -> { });
    }

    /** As above; every complete line of standard output is also handed to {@code onLine} while it runs. */
    static Result run(List<String> command, Path dir, Map<String, String> env, Path stdout,
            Path stderr, Duration timeout, BooleanSupplier cancelled, Consumer<String> onLine) {
        ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile())
                .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")))
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
        builder.environment().putAll(env);
        try {
            Process process = builder.start();
            // Wall clock, not nanoTime: nanoTime stops while the machine sleeps, so a step that hung
            // across a sleep would never time out.
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            LineTail tail = new LineTail(stdout, onLine);
            while (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                tail.poll();
                boolean stop = cancelled.getAsBoolean();
                if (stop || System.currentTimeMillis() > deadline) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                    tail.poll();
                    tail.finish();
                    return new Result(-1, !stop, stop);
                }
            }
            tail.poll();
            tail.finish();
            return new Result(process.exitValue(), false, false);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start " + command.get(0)
                    + "; is it installed and on the PATH?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + command.get(0), e);
        }
    }

    /** Reads the lines a growing file gains and hands each complete line on; at most 4000 characters each. */
    static final class LineTail {
        private static final int MAX_LINE = 4000;
        private final Path file;
        private final Consumer<String> onLine;
        private long offset;
        private final StringBuilder partial = new StringBuilder();

        LineTail(Path file, Consumer<String> onLine) {
            this.file = file;
            this.onLine = onLine;
        }

        void poll() {
            try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {
                long length = in.length();
                if (length <= offset) {
                    return;
                }
                byte[] bytes = new byte[(int) Math.min(length - offset, 1 << 20)];
                in.seek(offset);
                in.readFully(bytes);
                offset += bytes.length;
                partial.append(new String(bytes, StandardCharsets.UTF_8));
                int newline;
                while ((newline = partial.indexOf("\n")) >= 0) {
                    emit(partial.substring(0, newline));
                    partial.delete(0, newline + 1);
                }
            } catch (IOException e) {
                // the file is gone or unreadable: the result of the process still counts, not the preview
            }
        }

        void finish() {
            if (!partial.isEmpty()) {
                emit(partial.toString());
                partial.setLength(0);
            }
        }

        private void emit(String line) {
            String text = line.stripTrailing();
            if (!text.isEmpty()) {
                onLine.accept(text.length() > MAX_LINE ? text.substring(0, MAX_LINE) : text);
            }
        }
    }
}
