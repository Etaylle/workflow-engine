package engine.projects;

import java.util.List;

/**
 * The brief of a wish as Claude writes it (structured output by class) and as the API shows it:
 * the goal, who is involved, the terms of the domain and the risks seen.
 */
public record Brief(String goal, List<String> actors, List<String> domainTerms, List<String> risks) {

    /** The brief as plain text for the next planning calls. */
    String toText() {
        return "Goal: " + goal
                + "\nActors: " + String.join(", ", actors)
                + "\nDomain terms: " + String.join(", ", domainTerms)
                + "\nRisks: " + String.join("; ", risks);
    }

    /** Throws when the brief cannot be used. */
    static void validate(Brief brief) {
        if (brief == null || brief.goal() == null || brief.goal().isBlank()) {
            throw new engine.shared.InvalidAiOutputException("The brief has no goal.");
        }
        if (brief.actors() == null || brief.actors().isEmpty()) {
            throw new engine.shared.InvalidAiOutputException("The brief names no actors.");
        }
        if (brief.domainTerms() == null || brief.risks() == null) {
            throw new engine.shared.InvalidAiOutputException(
                    "The brief needs lists of domain terms and risks (they may be empty).");
        }
    }
}
