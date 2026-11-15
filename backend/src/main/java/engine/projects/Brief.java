package engine.projects;

import java.util.List;

/**
 * The brief of a wish as Claude writes it (structured output by class) and as the API shows it:
 * the goal, who is involved, the terms of the domain, the risks seen and the kind of software the
 * wish asks for (console program or web app).
 */
public record Brief(String goal, List<String> actors, List<String> domainTerms, List<String> risks,
        String softwareKind) {

    /** The brief as plain text for the next planning calls. */
    String toText() {
        return "Goal: " + goal
                + "\nActors: " + String.join(", ", actors)
                + "\nDomain terms: " + String.join(", ", domainTerms)
                + "\nRisks: " + String.join("; ", risks)
                + "\nKind of software: " + kindText();
    }

    private String kindText() {
        return "console".equals(softwareKind)
                ? "console program (output on the console, no web pages, no endpoints, no database unless"
                        + " the wish asks for data to be kept)"
                : "web app";
    }

    /** Throws when the brief cannot be used. */
    static void validate(Brief brief) {
        if (brief == null || brief.goal() == null || brief.goal().isBlank()) {
            throw new engine.shared.InvalidAiOutputException("The brief has no goal.");
        }
        if (brief.actors() == null || brief.actors().isEmpty()) {
            throw new engine.shared.InvalidAiOutputException("The brief names no actors.");
        }
        if (!"console".equals(brief.softwareKind()) && !"web".equals(brief.softwareKind())) {
            throw new engine.shared.InvalidAiOutputException(
                    "softwareKind must be \"console\" or \"web\".");
        }
        if (brief.domainTerms() == null || brief.risks() == null) {
            throw new engine.shared.InvalidAiOutputException(
                    "The brief needs lists of domain terms and risks (they may be empty).");
        }
    }
}
