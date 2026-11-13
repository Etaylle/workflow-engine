package engine.projects.application.ai;

import java.util.List;

/**
 * Structured output of the PROMPT AGENT (Spring AI: chatClient...call().entity(ProjectBriefOutput.class)).
 * Maps to projects.project_briefs: goal -> goal, actors -> actors, domainTerms -> domain_terms, risks -> risks.
 * Validation: goal not blank; 1..10 actors.
 */
public record ProjectBriefOutput(String goal, List<String> actors, List<String> domainTerms, List<String> risks) {
}
