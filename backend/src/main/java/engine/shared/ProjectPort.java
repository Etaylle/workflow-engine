package engine.shared;

import java.util.Optional;
import java.util.UUID;

/** What other domains may ask of the domain "projects" (domains do not call each other). */
public interface ProjectPort {

    /** The name of the project, or empty when the project does not exist. */
    Optional<String> nameOf(UUID projectId);

    /** The wish of the project, or empty when the project does not exist. */
    Optional<String> wishOf(UUID projectId);

    /** The status of the project, or empty when the project does not exist. */
    Optional<String> statusOf(UUID projectId);

    /** The brief of the project as text for a planning call, or empty when there is none. */
    Optional<String> briefOf(UUID projectId);

    /** The builder the project chose, or empty when the project does not exist. */
    Optional<Harness> harnessOf(UUID projectId);

    /** The id of the template the project chose, or empty when the project does not exist. */
    Optional<String> templateIdOf(UUID projectId);

    /** The build mode the client chose (whole or tasks), or empty when the contract decides. */
    Optional<String> buildModeOf(UUID projectId);

    /** The model and effort the project chose for its builder (the default when none was chosen). */
    Optional<BuilderChoice> builderChoiceOf(UUID projectId);

    /** Records which provider and model the planning of the project really uses. */
    void recordAi(UUID projectId, AiTarget target);

    /** Moves the project to the given status (a value of the status check of the table). */
    void markStatus(UUID projectId, String status);
}
