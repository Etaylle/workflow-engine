package engine.shared;

import java.util.UUID;

/** Implemented by each domain: removes what it holds for a project that is being deleted. */
public interface ProjectCleanup {

    /** Deletes every row of the domain that belongs to the project. */
    void deleteAllOf(UUID projectId);
}
