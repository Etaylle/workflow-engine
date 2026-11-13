package engine.documents.application.ai;

import engine.documents.domain.Priority;
import engine.documents.domain.RequirementType;
import java.util.List;

/**
 * Structured output of the REQUIREMENTS AGENT.
 * EpicDraft -> documents.epics (E-0 is always the template setup); RequirementDraft -> documents.requirements.
 * Validation (FR-AGT-06): keys unique per project, every requirement has an epic key that exists,
 * priority set, source names the wish or a question number.
 */
public record RequirementSetOutput(List<EpicDraft> epics, List<RequirementDraft> requirements) {

    public record EpicDraft(String key, String name, String domainName) {
    }

    public record RequirementDraft(String key, String epicKey, RequirementType type, String text,
            Priority priority, String source) {
    }
}
