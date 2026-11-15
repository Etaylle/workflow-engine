package engine.execution;

import engine.shared.ApiProblem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Read-only view of what a run built: the checkpoints (git commits) with their changed files, the
 * file tree and the text of one file at a checkpoint. Everything is read from the git history, never
 * from the working folder, so a half-finished step is not shown and a path cannot leave the workspace.
 */
final class WorkspaceFiles {

    /** The most a file may be for its text to be sent. */
    static final long MAX_FILE_BYTES = 256 * 1024;
    private static final int MAX_CHECKPOINTS = 100;
    private static final int MAX_FILES_PER_CHECKPOINT = 300;
    private static final int MAX_TREE = 5000;
    private static final int BINARY_PROBE = 8000;
    private static final Pattern REF = Pattern.compile("HEAD|[0-9a-f]{7,40}");

    /** One changed file of a checkpoint; binary files have no line counts. */
    record FileChange(String path, int added, int removed, boolean binary) {
    }

    /** One checkpoint: a commit of the workspace and the files it changed. */
    record Checkpoint(String sha, String message, Instant committedAt, int fileCount,
            List<FileChange> files) {
    }

    /** One file of the tree at a checkpoint. */
    record TreeEntry(String path, long size) {
    }

    /** The text of one file; binary and too large files come without text. */
    record FileContent(String path, long size, boolean binary, boolean tooLarge, String content) {
    }

    private final Workspace workspace;

    WorkspaceFiles(Workspace workspace) {
        this.workspace = workspace;
    }

    /** The latest checkpoints, newest first, each with the files it changed. */
    List<Checkpoint> checkpoints() {
        String log = workspace.git("log", "-n", String.valueOf(MAX_CHECKPOINTS), "--no-renames",
                "--numstat", "--format=%x01%H%x1f%s%x1f%cI");
        List<Checkpoint> result = new ArrayList<>();
        for (String block : log.split("\u0001")) {
            if (!block.isBlank()) {
                result.add(parseCheckpoint(block));
            }
        }
        return result;
    }

    private static Checkpoint parseCheckpoint(String block) {
        String[] lines = block.split("\n");
        String[] head = lines[0].split("\u001f", 3);
        List<FileChange> files = new ArrayList<>();
        int count = 0;
        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split("\t", 3);
            if (cols.length < 3) {
                continue;
            }
            count++;
            if (files.size() < MAX_FILES_PER_CHECKPOINT) {
                boolean binary = "-".equals(cols[0]);
                files.add(new FileChange(cols[2], binary ? 0 : Integer.parseInt(cols[0]),
                        binary ? 0 : Integer.parseInt(cols[1]), binary));
            }
        }
        return new Checkpoint(head[0], head[1], Instant.parse(head[2]), count, files);
    }

    /** All files of the checkpoint (HEAD or a commit id), with their sizes, sorted by path. */
    List<TreeEntry> tree(String ref) {
        String listing = workspace.git("ls-tree", "-r", "-l", "-z", checkedRef(ref));
        List<TreeEntry> entries = new ArrayList<>();
        for (String record : listing.split("\0")) {
            int tab = record.indexOf('\t');
            if (tab < 0 || !record.startsWith("100")) {
                continue;
            }
            String[] meta = record.substring(0, tab).trim().split("\\s+");
            entries.add(new TreeEntry(record.substring(tab + 1), Long.parseLong(meta[3])));
            if (entries.size() >= MAX_TREE) {
                break;
            }
        }
        entries.sort((a, b) -> a.path().compareTo(b.path()));
        return entries;
    }

    /** The text of a file at the checkpoint; the path must stay inside the workspace. */
    FileContent file(String ref, String path) {
        String safe = checkedPath(path);
        String object = checkedRef(ref) + ":" + safe;
        long size;
        try {
            size = Long.parseLong(workspace.git("cat-file", "-s", object).strip());
        } catch (IllegalStateException e) {
            throw new ApiProblem(404, "No such file", "The file " + safe + " does not exist at this checkpoint.");
        }
        if (!"blob".equals(workspace.git("cat-file", "-t", object).strip())) {
            throw new ApiProblem(404, "Not a file", safe + " is not a file.");
        }
        if (size > MAX_FILE_BYTES) {
            return new FileContent(safe, size, false, true, null);
        }
        byte[] bytes = workspace.gitBytes("cat-file", "blob", object);
        for (int i = 0; i < Math.min(bytes.length, BINARY_PROBE); i++) {
            if (bytes[i] == 0) {
                return new FileContent(safe, size, true, false, null);
            }
        }
        return new FileContent(safe, size, false, false, new String(bytes, StandardCharsets.UTF_8));
    }

    private static String checkedRef(String ref) {
        String value = ref == null || ref.isBlank() ? "HEAD" : ref;
        if (!REF.matcher(value).matches()) {
            throw new ApiProblem(400, "Bad checkpoint", "The checkpoint must be HEAD or a commit id.");
        }
        return value;
    }

    /** A relative path without empty, "." or ".." parts, backslashes or NUL; inside the workspace. */
    String checkedPath(String path) {
        if (path == null || path.isEmpty() || path.length() > 500 || path.startsWith("/")
                || path.indexOf('\0') >= 0 || path.indexOf('\\') >= 0) {
            throw bad(path);
        }
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw bad(path);
            }
        }
        Path root = workspace.dir().toAbsolutePath().normalize();
        if (!root.resolve(path).normalize().startsWith(root)) {
            throw bad(path);
        }
        return path;
    }

    private static ApiProblem bad(String path) {
        return new ApiProblem(400, "Bad path", "The path must be a file path inside the workspace: "
                + (path == null ? "" : path.replace('\0', '?')));
    }
}
