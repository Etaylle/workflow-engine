package engine.execution;

import engine.shared.Template;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Uses the built software the way a client would, after ./check.sh is green: starts it with
 * ./start.sh, then reads the output of a console program, or opens the page of a web app in a
 * browser, presses its main controls and collects script errors, failed requests and empty pages.
 * What it finds goes back to the builder like a failed check.
 */
@Component
class UseCheck {

    /** Outcome of using the software: passed, and the lines to show and to give to the builder. */
    record Result(boolean passed, List<String> lines) {
    }

    private static final String SCRIPT = "start.sh";
    /** Folder of the workspace with the pictures of the software in use. */
    static final String SCREENS = "screens";
    private static final long POLL_MS = 300;
    private static final int TAIL = 15;
    /** Exit code of the browser script when no browser tooling is installed. */
    private static final int NO_BROWSER = 2;

    private final boolean enabled;
    private final Duration startTimeout;
    private final Duration consoleTime;
    private final Path browserScript;
    private final Path nodePath;

    UseCheck(@Value("${engine.execution.use-check:true}") boolean enabled,
            @Value("${engine.execution.use-check-start-seconds:300}") long startSeconds,
            @Value("${engine.execution.use-check-console-seconds:12}") long consoleSeconds,
            @Value("${engine.execution.browser-script:tools/use-check/browser.cjs}") String browserScript,
            @Value("${engine.execution.browser-node-path:frontend/node_modules}") String nodePath) {
        this.enabled = enabled;
        this.startTimeout = Duration.ofSeconds(startSeconds);
        this.consoleTime = Duration.ofSeconds(consoleSeconds);
        this.browserScript = Path.of(browserScript).toAbsolutePath();
        this.nodePath = Path.of(nodePath).toAbsolutePath();
    }

    boolean enabled() {
        return enabled;
    }

    /** Starts the software of the workspace and uses it; always ends the process it started. */
    Result run(Template template, Path workspace, Map<String, String> appEnv, Path logDir,
            BooleanSupplier stopped, Consumer<String> console) {
        if (!Files.isRegularFile(workspace.resolve(SCRIPT))) {
            return failed(List.of("./start.sh is missing: one command must start the software"));
        }
        boolean isConsole = Template.CONSOLE.equals(template.kindIn(workspace));
        Path out = logDir.resolve("use-check.out");
        Path err = logDir.resolve("use-check.err");
        Integer port = isConsole ? null : RunPreviews.freePort();
        console.accept("use: starting the software with ./start.sh"
                + (port == null ? "" : " on port " + port));
        Process process = launch(workspace, appEnv, port, out, err);
        try {
            Result result = isConsole ? useConsole(process, out, err, stopped)
                    : useWeb(process, port, out, err, logDir, workspace, stopped);
            result.lines().forEach(line -> console.accept("use: " + line));
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failed(List.of("The use check was interrupted"));
        } finally {
            RunPreviews.kill(process);
        }
    }

    /** A console program must print something and must not end with an error. */
    private Result useConsole(Process process, Path out, Path err, BooleanSupplier stopped)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(startTimeout);
        Instant firstOutput = null;
        while (process.isAlive() && Instant.now().isBefore(deadline) && !stopped.getAsBoolean()) {
            if (firstOutput == null && size(out) > 0) {
                firstOutput = Instant.now();
            }
            if (firstOutput != null && Instant.now().isAfter(firstOutput.plus(consoleTime))) {
                break;
            }
            Thread.sleep(POLL_MS);
        }
        List<String> output = tail(out);
        if (!process.isAlive() && process.exitValue() != 0) {
            List<String> lines = new ArrayList<>(List.of("./start.sh ended with exit code "
                    + process.exitValue() + " (it ran without a key press or other input). Output:"));
            lines.addAll(output);
            lines.addAll(tail(err));
            return failed(lines);
        }
        if (size(out) == 0) {
            List<String> lines = new ArrayList<>(List.of(process.isAlive()
                    ? "./start.sh printed nothing within " + startTimeout.toSeconds() + " seconds"
                    : "./start.sh ended without printing anything"));
            lines.addAll(tail(err));
            return failed(lines);
        }
        List<String> lines = new ArrayList<>(List.of(process.isAlive()
                ? "the program runs and prints (it waits for input or keeps running)"
                : "the program printed its output and ended with exit code 0"));
        lines.addAll(output.subList(Math.max(0, output.size() - 5), output.size()));
        return new Result(true, lines);
    }

    /** A web app must answer on its port, and its page must work when opened and used. */
    private Result useWeb(Process process, int port, Path out, Path err, Path logDir, Path workspace,
            BooleanSupplier stopped) throws InterruptedException {
        Instant deadline = Instant.now().plus(startTimeout);
        while (!RunPreviews.answers(port)) {
            if (!process.isAlive()) {
                return failedWithOutput("./start.sh ended with exit code " + process.exitValue()
                        + " before the app answered on port " + port + " (APP_PORT). Output:", out, err);
            }
            if (Instant.now().isAfter(deadline) || stopped.getAsBoolean()) {
                return failedWithOutput("The app did not answer on port " + port + " (APP_PORT) within "
                        + startTimeout.toSeconds() + " seconds after ./start.sh. Output:", out, err);
            }
            Thread.sleep(POLL_MS);
        }
        String url = "http://localhost:" + port + "/";
        int status = statusOf(url);
        if (status >= 400 || status < 0) {
            return failedWithOutput("The start page " + url + " answered "
                    + (status < 0 ? "nothing" : "HTTP " + status) + ". Output:", out, err);
        }
        return browse(url, logDir, workspace);
    }

    /** Opens the page in a headless browser through the browser script and reads its findings. */
    private Result browse(String url, Path logDir, Path workspace) {
        if (!Files.isRegularFile(browserScript)) {
            return new Result(true, List.of("the app answers at " + url
                    + "; browser check skipped: " + browserScript + " is missing"));
        }
        Path out = logDir.resolve("use-check-browser.out");
        ProcessRunner.Result result = ProcessRunner.run(
                List.of("node", browserScript.toString(), url, logDir.resolve("use-check.png").toString()),
                logDir, Map.of("NODE_PATH", nodePath.toString()), out, out, Duration.ofSeconds(120));
        List<String> lines = BuilderSupport.read(out).lines().filter(line -> !line.isBlank()).toList();
        int pictures = keepPictures(logDir, workspace);
        if (result.exitCode() == NO_BROWSER) {
            return new Result(true, List.of("the app answers at " + url
                    + "; browser check skipped: no browser tooling in " + nodePath));
        }
        List<String> findings = lines.stream().filter(line -> line.startsWith("FINDING: "))
                .map(line -> line.substring("FINDING: ".length())).toList();
        if (result.timedOut() || (result.exitCode() != 0 && findings.isEmpty())) {
            List<String> all = new ArrayList<>(List.of("The browser could not use the page " + url + ":"));
            all.addAll(lines.subList(Math.max(0, lines.size() - TAIL), lines.size()));
            return failed(all);
        }
        if (!findings.isEmpty()) {
            List<String> all = new ArrayList<>(List.of("Opening " + url + " in a browser and pressing"
                    + " its main controls (start button, arrow keys, space, enter) showed:"));
            all.addAll(findings);
            if (pictures > 0) {
                all.add("Pictures of what the browser showed are in " + SCREENS + "/ (look at them).");
            }
            return failed(all);
        }
        List<String> info = new ArrayList<>(List.of("the page works in a browser: no script error,"
                + " no failed request, not empty"));
        lines.stream().filter(line -> line.startsWith("INFO: ")).map(line -> line.substring(6))
                .forEach(info::add);
        if (pictures > 0) {
            info.add(pictures + " pictures of the page in use are in " + SCREENS + "/");
        }
        return new Result(true, info);
    }

    /**
     * Copies the pictures the browser took into the workspace, so the builder of the next step can
     * look at what a client sees.
     */
    private static int keepPictures(Path logDir, Path workspace) {
        Path target = workspace.resolve(SCREENS);
        int kept = 0;
        try (java.util.stream.Stream<Path> files = Files.list(logDir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().matches("use-check.*\\.png"))
                    .sorted().toList()) {
                Files.createDirectories(target);
                Files.copy(file, target.resolve(file.getFileName().toString().replace("use-check", "use")),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                kept++;
            }
        } catch (IOException e) {
            return kept;
        }
        return kept;
    }

    private static Process launch(Path workspace, Map<String, String> appEnv, Integer port, Path out,
            Path err) {
        ProcessBuilder builder = new ProcessBuilder("bash", SCRIPT).directory(workspace.toFile())
                .redirectOutput(out.toFile()).redirectError(err.toFile());
        Map<String, String> env = new HashMap<>(appEnv);
        if (port != null) {
            env.put("APP_PORT", String.valueOf(port));
            env.put("PORT", String.valueOf(port));
        }
        builder.environment().putAll(env);
        try {
            Files.deleteIfExists(out);
            Files.deleteIfExists(err);
            return builder.start();
        } catch (IOException e) {
            throw new IllegalStateException("Could not start ./start.sh: " + e.getMessage(), e);
        }
    }

    private static int statusOf(String url) {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                    .GET().build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static Result failedWithOutput(String first, Path out, Path err) {
        List<String> lines = new ArrayList<>(List.of(first));
        lines.addAll(tail(out));
        lines.addAll(tail(err));
        return failed(lines);
    }

    private static Result failed(List<String> lines) {
        return new Result(false, lines.stream().map(line -> BuilderStream.redact(line, List.of())).toList());
    }

    private static long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    private static List<String> tail(Path file) {
        List<String> lines = BuilderSupport.read(file).lines().filter(line -> !line.isBlank()).toList();
        return lines.subList(Math.max(0, lines.size() - TAIL), lines.size());
    }
}
