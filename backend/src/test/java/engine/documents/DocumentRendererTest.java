package engine.documents;

import static org.assertj.core.api.Assertions.assertThat;

import engine.documents.DocumentModel.Section;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class DocumentRendererTest {

    private final DocumentModel doc = new DocumentModel("Plan <A&B>", List.of(
            new Section("Goal", List.of("Order lunch"), List.of("one", "two"))));

    @Test
    void markdownHasTitleHeadingParagraphAndBullets() {
        assertThat(DocumentRenderer.markdown(doc))
                .contains("# Plan <A&B>", "## Goal", "Order lunch", "- one", "- two");
    }

    @Test
    void docxIsAZipWithEscapedText() throws Exception {
        byte[] bytes = DocumentRenderer.docx(doc);
        String xml = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                if (e.getName().equals("word/document.xml")) {
                    xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        assertThat(xml).contains("Plan &lt;A&amp;B&gt;", "Order lunch", "• one");
    }

    @Test
    void anEmptyDocumentStillRenders() {
        DocumentModel empty = new DocumentModel("Empty", List.of());
        assertThat(DocumentRenderer.markdown(empty)).isEqualTo("# Empty\n");
        assertThat(DocumentRenderer.docx(empty)).isNotEmpty();
    }
}
