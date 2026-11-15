package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import engine.shared.ApiProblem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real git in a temporary workspace: checkpoints, tree, file text and every way out of the folder. */
class WorkspaceFilesTest {

    @TempDir Path root;
    private Workspace workspace;
    private WorkspaceFiles files;
    private Path secret;

    @BeforeEach
    void setUp() throws IOException {
        Path template = Files.createDirectories(root.resolve("template/src"));
        Files.writeString(template.resolve("Duck.java"), "class Duck {}\n");
        secret = Files.writeString(root.resolve("secret.txt"), "top secret\n");
        workspace = new Workspace(root.resolve("ws"));
        workspace.prepare(root.resolve("template"), "# contract\n");
        files = new WorkspaceFiles(workspace);
    }

    @Test
    void checkpointsListTheChangedFilesWithLineCountsNewestFirst() throws IOException {
        Files.writeString(workspace.dir().resolve("src/Duck.java"), "class Duck {}\nclass Pond {}\n");
        Files.write(workspace.dir().resolve("logo.bin"), new byte[] {1, 0, 2});
        workspace.commit("Task 1 step build green");

        List<WorkspaceFiles.Checkpoint> checkpoints = files.checkpoints();

        assertThat(checkpoints).extracting(WorkspaceFiles.Checkpoint::message)
                .containsExactly("Task 1 step build green", "Template and contract");
        assertThat(checkpoints.get(0).files()).containsExactlyInAnyOrder(
                new WorkspaceFiles.FileChange("src/Duck.java", 1, 0, false),
                new WorkspaceFiles.FileChange("logo.bin", 0, 0, true));
        assertThat(checkpoints.get(1).files()).extracting(WorkspaceFiles.FileChange::path)
                .contains("src/Duck.java", "CONTRACT.md");
    }

    @Test
    void theTreeAndTheFileShowTheLatestCheckpointNotTheHalfFinishedWork() throws IOException {
        Files.writeString(workspace.dir().resolve("src/Duck.java"), "uncommitted change\n");
        Files.writeString(workspace.dir().resolve("Draft.java"), "not committed\n");

        assertThat(files.tree(null)).extracting(WorkspaceFiles.TreeEntry::path)
                .contains("CONTRACT.md", "src/Duck.java").doesNotContain("Draft.java");
        assertThat(files.file("HEAD", "src/Duck.java").content()).isEqualTo("class Duck {}\n");
        assertThat(files.tree("HEAD")).noneMatch(e -> e.path().startsWith(".git/"));
    }

    @Test
    void anOlderCheckpointShowsTheOlderText() throws IOException {
        String first = files.checkpoints().get(0).sha();
        Files.writeString(workspace.dir().resolve("src/Duck.java"), "class Duck2 {}\n");
        workspace.commit("second");

        assertThat(files.file(first, "src/Duck.java").content()).isEqualTo("class Duck {}\n");
        assertThat(files.file(null, "src/Duck.java").content()).isEqualTo("class Duck2 {}\n");
    }

    @Test
    void binaryAndOversizedFilesComeWithoutText() throws IOException {
        Files.write(workspace.dir().resolve("logo.png"), new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, 1});
        Files.write(workspace.dir().resolve("big.txt"),
                "x".repeat((int) WorkspaceFiles.MAX_FILE_BYTES + 1).getBytes());
        workspace.commit("assets");

        WorkspaceFiles.FileContent binary = files.file(null, "logo.png");
        WorkspaceFiles.FileContent big = files.file(null, "big.txt");

        assertThat(binary.binary()).isTrue();
        assertThat(binary.content()).isNull();
        assertThat(big.tooLarge()).isTrue();
        assertThat(big.content()).isNull();
        assertThat(big.size()).isEqualTo(WorkspaceFiles.MAX_FILE_BYTES + 1);
    }

    @Test
    void pathsThatLeaveTheWorkspaceAreRefused() {
        for (String path : List.of("../secret.txt", "src/../../secret.txt", "..", "/etc/passwd",
                secret.toString(), "src\\..\\x", "src//Duck.java", "./src/Duck.java", "a\0b", "",
                "src/./Duck.java")) {
            assertThatThrownBy(() -> files.file(null, path)).as(path).isInstanceOf(ApiProblem.class)
                    .extracting(e -> ((ApiProblem) e).status()).isEqualTo(400);
        }
        assertThatThrownBy(() -> files.file(null, null)).isInstanceOf(ApiProblem.class);
    }

    @Test
    void aSymlinkInsideTheWorkspaceShowsItsTargetNameNeverTheFileItPointsTo() throws IOException {
        Files.createSymbolicLink(workspace.dir().resolve("link.txt"), secret);
        workspace.commit("link");

        WorkspaceFiles.FileContent link = files.file(null, "link.txt");

        assertThat(link.content()).doesNotContain("top secret");
        assertThat(files.tree(null)).noneMatch(e -> e.path().equals("link.txt"));
    }

    @Test
    void unknownFilesAndDirectoriesAreNotFound() {
        assertThatThrownBy(() -> files.file(null, "nope.txt")).isInstanceOf(ApiProblem.class)
                .extracting(e -> ((ApiProblem) e).status()).isEqualTo(404);
        assertThatThrownBy(() -> files.file(null, "src")).isInstanceOf(ApiProblem.class)
                .extracting(e -> ((ApiProblem) e).status()).isEqualTo(404);
        assertThatThrownBy(() -> files.file(null, ".git/config")).isInstanceOf(ApiProblem.class)
                .extracting(e -> ((ApiProblem) e).status()).isEqualTo(404);
    }

    @Test
    void aCheckpointThatIsNotACommitIdIsRefused() {
        for (String ref : List.of("--output=/tmp/x", "HEAD;ls", "main", "HEAD~1", "zzzzzzz")) {
            assertThatThrownBy(() -> files.tree(ref)).as(ref).isInstanceOf(ApiProblem.class)
                    .extracting(e -> ((ApiProblem) e).status()).isEqualTo(400);
            assertThatThrownBy(() -> files.file(ref, "src/Duck.java")).as(ref)
                    .isInstanceOf(ApiProblem.class);
        }
    }
}
