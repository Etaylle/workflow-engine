package engine.execution;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** The folder of one run: copy of the template, contract, git checkpoints, ZIP. */
final class Workspace {

    private static final Set<String> SKIPPED_NAMES =
            Set.of("node_modules", "build", ".gradle", ".svelte-kit", "dist");
    /** Name of the run report inside the ZIP. */
    static final String REPORT_FILE = "RUN-REPORT.md";
    private static final Duration GIT_TIMEOUT = Duration.ofSeconds(60);

    private final Path dir;

    Workspace(Path dir) {
        this.dir = dir;
    }

    Path dir() {
        return dir;
    }

    /** Copies the template, writes the contract and makes the first commit. */
    void prepare(Path template, String contract) {
        if (!Files.isDirectory(template)) {
            throw new IllegalStateException("The project template is missing: " + template
                    + " (set ENGINE_TEMPLATE_DIR)");
        }
        try {
            copy(template, dir);
            Files.createDirectories(dir.resolve("docs"));
            Files.writeString(dir.resolve("CONTRACT.md"), contract, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not set up the workspace " + dir, e);
        }
        git("init", "-q");
        git("config", "user.name", "workflow-engine");
        git("config", "user.email", "engine@localhost");
        commit("Template and contract");
    }

    /** Adds the task to build to the contract text the builder reads. */
    void writeTask(String contract, String title, String description, List<String> done,
            List<String> later) {
        StringBuilder text = new StringBuilder(contract).append("\n## The task to build in this run\n\n")
                .append("Task \"").append(title).append("\"");
        if (description != null && !description.isBlank()) {
            text.append(". Description from the contract: \"").append(description).append("\"");
        }
        text.append(".\n\nThe result must be visible in the running application and its data kept in"
                + " PostgreSQL.\n");
        if (!done.isEmpty()) {
            text.append("\nAlready built in earlier tasks (keep it working): ")
                    .append(String.join(", ", done)).append(".\n");
        }
        if (!later.isEmpty()) {
            text.append("\nBuilt later and out of scope now: ").append(String.join(", ", later))
                    .append(".\n");
        }
        try {
            Files.writeString(dir.resolve("CONTRACT.md"), text.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the task into CONTRACT.md", e);
        }
    }

    /** Commits everything and returns the commit id. */
    String commit(String message) {
        git("add", "-A");
        git("commit", "-q", "--allow-empty", "-m", message);
        return git("rev-parse", "HEAD").strip();
    }

    /** Throws away everything after the last commit (the last green checkpoint), files and changes. */
    void resetToCheckpoint() {
        git("reset", "-q", "--hard", "HEAD");
        git("clean", "-q", "-fd");
    }

    String git(String... args) {
        return new String(gitBytes(args), StandardCharsets.UTF_8);
    }

    /** Runs git in the workspace and returns its standard output; a failure is an exception. */
    byte[] gitBytes(String... args) {
        List<String> command = new java.util.ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        try {
            Path out = Files.createTempFile("engine-git", ".out");
            Path err = Files.createTempFile("engine-git", ".err");
            try {
                ProcessRunner.Result result =
                        ProcessRunner.run(command, dir, Map.of(), out, err, GIT_TIMEOUT);
                if (result.exitCode() != 0) {
                    throw new IllegalStateException("git " + args[0] + " failed in " + dir + ": "
                            + Files.readString(err).strip());
                }
                return Files.readAllBytes(out);
            } finally {
                Files.deleteIfExists(out);
                Files.deleteIfExists(err);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("git " + args[0] + " could not run", e);
        }
    }

    /**
     * Writes the workspace as ZIP without .git, build output and node_modules, plus one extra text
     * file (the run report) that replaces a workspace file of the same name.
     */
    void zip(OutputStream target, String extraName, String extraText) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(target, StandardCharsets.UTF_8);
                Stream<Path> files = Files.walk(dir)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)
                    .filter(this::included)::iterator) {
                String name = dir.relativize(file).toString().replace('\\', '/');
                if (name.equals(extraName)) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(name));
                Files.copy(file, zip);
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry(extraName));
            zip.write(extraText.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private boolean included(Path file) {
        for (Path part : dir.relativize(file)) {
            String name = part.toString();
            if (name.equals(".git") || SKIPPED_NAMES.contains(name)) {
                return false;
            }
        }
        return true;
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path source : (Iterable<Path>) files::iterator) {
                Path relative = from.relativize(source);
                if (relative.toString().isEmpty()) {
                    Files.createDirectories(to);
                    continue;
                }
                if (skipped(relative)) {
                    continue;
                }
                Path target = to.resolve(relative);
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private static boolean skipped(Path relative) {
        for (Path part : relative) {
            if (SKIPPED_NAMES.contains(part.toString()) || part.toString().equals(".git")) {
                return true;
            }
        }
        return false;
    }
}
