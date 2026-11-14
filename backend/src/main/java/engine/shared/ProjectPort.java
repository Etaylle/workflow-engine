package engine.shared;

import java.util.Optional;
import java.util.UUID;

/** What other domains may ask of the domain "projects" (domains do not call each other). */
public interface ProjectPort {

    /** The wish of the project, or empty when the project does not exist. */
    Optional<String> wishOf(UUID projectId);

    /** Moves the project to the given status (a value of the status check of the table). */
    void markStatus(UUID projectId, String status);
}
