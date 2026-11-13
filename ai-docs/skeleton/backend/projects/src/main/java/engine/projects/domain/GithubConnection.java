package engine.projects.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * GitHub account of the person who started the project: each finished task is pushed to a public repository there, created on the first push. The token is never stored: it lives in engine memory for the session and the run only.
 * Table: projects.github_connections. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record GithubConnection(
        UUID projectId,
        String accountLogin,
        String repositoryName,
        RepoVisibility visibility,
        String branch,
        boolean pushAfterEachTask,
        String repositoryUrl,
        Instant repositoryCreatedAt,
        Instant createdAt,
        Instant updatedAt) {
}
