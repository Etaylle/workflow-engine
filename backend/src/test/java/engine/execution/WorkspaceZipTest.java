package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceZipTest {

    @TempDir Path root;

    @Test
    void theZipKeepsTheExecutableBitOfScriptsAndLeavesOutGitAndBuildOutput() throws IOException {
        Path dir = Files.createDirectories(root.resolve("ws/backend"));
        Files.createDirectories(root.resolve("ws/.git"));
        Files.createDirectories(root.resolve("ws/node_modules"));
        Files.writeString(root.resolve("ws/.git/config"), "x");
        Files.writeString(root.resolve("ws/node_modules/x.js"), "x");
        write(root.resolve("ws/start.sh"), "rwxr-xr-x");
        write(root.resolve("ws/check.sh"), "rwxr-xr-x");
        write(dir.resolve("gradlew"), "rwxr-xr-x");
        write(root.resolve("ws/README.md"), "rw-r--r--");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        new Workspace(root.resolve("ws")).zip(out, "RUN-REPORT.md", "report");

        try (ZipFile zip = ZipFile.builder()
                .setSeekableByteChannel(new SeekableInMemoryByteChannel(out.toByteArray())).get()) {
            assertThat(mode(zip, "start.sh")).isEqualTo(0755);
            assertThat(mode(zip, "check.sh")).isEqualTo(0755);
            assertThat(mode(zip, "backend/gradlew")).isEqualTo(0755);
            assertThat(mode(zip, "README.md")).isEqualTo(0644);
            assertThat(mode(zip, "RUN-REPORT.md")).isEqualTo(0644);
            assertThat(zip.getEntry(".git/config")).isNull();
            assertThat(zip.getEntry("node_modules/x.js")).isNull();
        }
    }

    private static void write(Path file, String permissions) throws IOException {
        Files.writeString(file, "content");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(permissions));
    }

    private static int mode(ZipFile zip, String name) {
        ZipArchiveEntry entry = zip.getEntry(name);
        assertThat(entry).as(name).isNotNull();
        return entry.getUnixMode() & 07777;
    }
}
