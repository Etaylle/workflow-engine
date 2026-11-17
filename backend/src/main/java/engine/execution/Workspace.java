package engine.execution;

import engine.shared.Template;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

/** The folder of one run: copy of the template, contract, git checkpoints, ZIP. */
final class Workspace {

    private static final Set<String> SKIPPED_NAMES =
            Set.of("node_modules", "build", ".gradle", ".svelte-kit", "dist");
    /** Name of the run report inside the ZIP. */
    static final String REPORT_FILE = "RUN-REPORT.md";
    /** The file type bits of a regular file in a Unix mode. */
    private static final int REGULAR_FILE = 0100000;
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
                    + " ");
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

    static final String MANUAL_RULE = " Steps of the workflow done by a person (manual tasks, for example a"
            + " player controlling a character) are user actions that the software must offer"
            + " (controls, inputs, screens); they are part of what is built.";

    static String resultRule(Template template) {
        if (template.isFree()) {
            return "The result must be what the wish asks for as a whole, started with ./start.sh. You"
                    + " choose the kind of software and the technology that fit the wish (see the guide"
                    + " file) and declare the kind in app.json. Store data only where the wish or a"
                    + " requirement needs it to outlive the session; otherwise keep state in memory, and do"
                    + " not add a server, a database or network calls that the wish does not need."
                    + MANUAL_RULE;
        }
        if (template.isConsole()) {
            return "This is a console program: the result must be what the wish asks for as a whole,"
                    + " visible in the output of ./start.sh. No server, no database, no web page."
                    + MANUAL_RULE;
        }
        return "The result must be what the wish asks for as a whole, visible in the running application"
                + " and started with ./start.sh. Store data only where the wish or a requirement needs it"
                + " to outlive the session; otherwise keep state in memory, and do not add server routes,"
                + " tables or network calls that the wish does not need." + MANUAL_RULE;
    }

    /** Writes the contract with the one unit of the whole mode: the complete software in one go. */
    void writeWhole(String contract, Template template, String scope) {
        String text = contract + "\n## What to build in this run\n\nBuild the complete, working software"
                + " of this contract in one go. Read the wish first (section Wish), then the requirements,"
                + " then the workflow as the description of the behaviour (section Workflow). Scope: "
                + scope + "\n\nWhen the wish names a kind of software with a well-known design (for example"
                + " a classic game), build that known design fully: start, play, win and lose.\n\n"
                + resultRule(template) + "\n";
        try {
            Files.writeString(dir.resolve("CONTRACT.md"), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the task into CONTRACT.md", e);
        }
    }

    /** Adds the task to build to the contract text the builder reads. */
    void writeTask(String contract, Template template, String title, String description, List<String> done,
            List<String> later) {
        StringBuilder text = new StringBuilder(contract).append("\n## The task to build in this run\n\n")
                .append("Task \"").append(title).append("\"");
        if (description != null && !description.isBlank()) {
            text.append(". Description from the contract: \"").append(description).append("\"");
        }
        text.append(".\n\n").append(resultRule(template)).append("\n");
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

    private static final int MAP_FILES = 200;
    private static final long MAP_COUNTED_BYTES = 512 * 1024;

    /**
     * The files of the workspace with their line counts, as the end of a prompt: the builder then
     * knows what is there without listing and measuring it first. At most 200 files are named.
     */
    String fileMap() {
        List<String> files = git("ls-files", "--cached", "--others", "--exclude-standard").lines()
                .filter(path -> !path.isBlank() && Files.isRegularFile(dir.resolve(path))).sorted().toList();
        if (files.isEmpty()) {
            return "";
        }
        StringBuilder map = new StringBuilder("\n\nFiles in the workspace now (path, lines):");
        files.stream().limit(MAP_FILES).forEach(path -> map.append("\n- ").append(path).append(' ')
                .append(lineCount(dir.resolve(path))));
        if (files.size() > MAP_FILES) {
            map.append("\n- and ").append(files.size() - MAP_FILES).append(" more");
        }
        return map.toString();
    }

    /** The lines of a text file; "(binary)" or "(large)" where counting lines makes no sense. */
    private static String lineCount(Path file) {
        try {
            if (Files.size(file) > MAP_COUNTED_BYTES) {
                return "(large)";
            }
            return String.valueOf(Files.readString(file, StandardCharsets.UTF_8).lines().count());
        } catch (java.nio.charset.CharacterCodingException e) {
            return "(binary)";
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file + " for the file map", e);
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
     * file (the run report) that replaces a workspace file of the same name. The entries keep their
     * Unix permissions, so ./check.sh and ./start.sh still run after unzipping.
     */
    void zip(OutputStream target, String extraName, String extraText) throws IOException {
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(target);
                Stream<Path> files = Files.walk(dir)) {
            zip.setEncoding(StandardCharsets.UTF_8.name());
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)
                    .filter(this::included)::iterator) {
                String name = dir.relativize(file).toString().replace('\\', '/');
                if (name.equals(extraName)) {
                    continue;
                }
                ZipArchiveEntry entry = new ZipArchiveEntry(name);
                entry.setUnixMode(REGULAR_FILE | unixMode(file));
                zip.putArchiveEntry(entry);
                Files.copy(file, zip);
                zip.closeArchiveEntry();
            }
            ZipArchiveEntry report = new ZipArchiveEntry(extraName);
            report.setUnixMode(REGULAR_FILE | 0644);
            zip.putArchiveEntry(report);
            zip.write(extraText.getBytes(StandardCharsets.UTF_8));
            zip.closeArchiveEntry();
        }
    }

    /** The permission bits of the file as a Unix mode, 0644 where the file system has none. */
    private static int unixMode(Path file) throws IOException {
        int mode = 0;
        try {
            for (PosixFilePermission permission : Files.getPosixFilePermissions(file)) {
                mode |= 1 << (8 - permission.ordinal());
            }
            return mode;
        } catch (UnsupportedOperationException e) {
            return 0644;
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
