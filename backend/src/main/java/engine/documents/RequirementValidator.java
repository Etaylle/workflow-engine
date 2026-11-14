package engine.documents;

import engine.shared.InvalidAiOutputException;
import java.util.Set;

/** Checks the requirements Claude wrote; the message of a failure is sent back on the retry. */
final class RequirementValidator {

    static final int MAX_EPICS = 6;
    static final int MAX_REQUIREMENTS = 40;
    private static final Set<String> TYPES = Set.of("functional", "quality", "constraint");
    private static final Set<String> PRIORITIES = Set.of("must", "should", "could", "wont");

    private RequirementValidator() {
    }

    static void validate(RequirementDraft draft) {
        if (draft == null || draft.epics() == null || draft.epics().isEmpty()) {
            throw new InvalidAiOutputException("There are no epics.");
        }
        if (draft.epics().size() > MAX_EPICS) {
            throw new InvalidAiOutputException("There are more than 6 epics.");
        }
        int total = 0;
        for (RequirementDraft.Epic epic : draft.epics()) {
            if (epic.name() == null || epic.name().isBlank()
                    || epic.domainName() == null || epic.domainName().isBlank()) {
                throw new InvalidAiOutputException("An epic has no name or no domainName.");
            }
            if (epic.requirements() == null || epic.requirements().isEmpty()) {
                throw new InvalidAiOutputException("Epic '" + epic.name() + "' has no requirements.");
            }
            for (RequirementDraft.Item item : epic.requirements()) {
                item(item, epic.name());
            }
            total += epic.requirements().size();
        }
        if (total > MAX_REQUIREMENTS) {
            throw new InvalidAiOutputException("There are " + total + " requirements; at most 40.");
        }
    }

    private static void item(RequirementDraft.Item item, String epic) {
        if (item.text() == null || item.text().isBlank()) {
            throw new InvalidAiOutputException("A requirement of epic '" + epic + "' has no text.");
        }
        if (!TYPES.contains(item.type())) {
            throw new InvalidAiOutputException("Requirement type '" + item.type()
                    + "'; use functional, quality or constraint.");
        }
        if (!PRIORITIES.contains(item.priority())) {
            throw new InvalidAiOutputException("Requirement priority '" + item.priority()
                    + "'; use must, should, could or wont.");
        }
    }
}
