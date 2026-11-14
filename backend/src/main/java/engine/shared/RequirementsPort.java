package engine.shared;

import java.util.List;
import java.util.UUID;

/** What other domains may read of the domain "documents". */
public interface RequirementsPort {

    /** One requirement with key, type (functional, quality, constraint), priority and its epic. */
    record RequirementLine(String key, String type, String priority, String epic, String text) {
    }

    /** The requirements of the project in order; empty when not generated yet. */
    List<RequirementLine> requirementsOf(UUID projectId);
}
