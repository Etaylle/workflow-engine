package engine.projects.application;

import java.util.Optional;

/**
 * Secrets of one browser session, held in engine memory only (ADR-033, ADR-035).
 * Nothing here is ever written to the database, logs, files, the workspace or the ZIP.
 * A new session or an engine restart starts empty; the user enters the key again.
 * Skeleton: keep in step with ai-docs/software-elements.md.
 */
public interface SessionSecrets {

    /** Stores the Anthropic API key for this session after one successful test call. */
    void putApiKey(String sessionId, String apiKey);

    Optional<String> apiKey(String sessionId);

    /** Stores the GitHub token of the person who signed in (device flow, scope public_repo). */
    void putGithubToken(String sessionId, String token);

    Optional<String> githubToken(String sessionId);

    /** Forgets everything of this session (sign-out, session end). */
    void forget(String sessionId);
}
