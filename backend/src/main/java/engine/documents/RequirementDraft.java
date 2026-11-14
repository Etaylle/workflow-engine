package engine.documents;

import java.util.List;

/**
 * The requirements as Claude writes them (structured output by class), grouped by epic (one epic
 * per domain of the software). Type is functional, quality or constraint; priority is must,
 * should, could or wont; source says where it comes from (wish, or an answer).
 */
public record RequirementDraft(List<Epic> epics) {

    /** One domain of the software with its requirements. */
    public record Epic(String name, String domainName, List<Item> requirements) {
    }

    /** One requirement. */
    public record Item(String type, String priority, String text, String source) {
    }
}
