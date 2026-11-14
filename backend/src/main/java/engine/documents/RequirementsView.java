package engine.documents;

import java.util.List;

/** The requirements of a project grouped by epic, as the API shows them. */
public record RequirementsView(List<Epic> epics) {

    /** One epic with its requirements. */
    public record Epic(String key, String name, String domainName, List<Requirement> requirements) {
    }

    /** One requirement with its key (FR-001, QR-001, CON-001). */
    public record Requirement(String key, String type, String priority, String text, String source) {
    }
}
