package engine.documents;

import java.util.List;

/** A project document before it is written as JSON, Markdown or DOCX. */
public record DocumentModel(String title, List<Section> sections) {

    /** A heading with paragraphs and bullet points; either list may be empty. */
    public record Section(String heading, List<String> paragraphs, List<String> bullets) {
    }
}
