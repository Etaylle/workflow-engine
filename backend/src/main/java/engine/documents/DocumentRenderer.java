package engine.documents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes a {@link DocumentModel} as Markdown or as a minimal DOCX (no extra library). */
final class DocumentRenderer {

    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    private DocumentRenderer() {
    }

    static String markdown(DocumentModel doc) {
        StringBuilder out = new StringBuilder("# ").append(doc.title()).append("\n");
        for (DocumentModel.Section section : doc.sections()) {
            out.append("\n## ").append(section.heading()).append("\n");
            section.paragraphs().forEach(p -> out.append("\n").append(p).append("\n"));
            if (!section.bullets().isEmpty()) {
                out.append("\n");
                section.bullets().forEach(b -> out.append("- ").append(b).append("\n"));
            }
        }
        return out.toString();
    }

    static byte[] docx(DocumentModel doc) {
        StringBuilder body = new StringBuilder();
        body.append(paragraph(doc.title(), 36, true, false));
        for (DocumentModel.Section section : doc.sections()) {
            body.append(paragraph(section.heading(), 28, true, false));
            section.paragraphs().forEach(p -> body.append(paragraph(p, 22, false, false)));
            section.bullets().forEach(b -> body.append(paragraph(b, 22, false, true)));
        }
        String document = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document xmlns:w=\"" + W + "\"><w:body>" + body + "</w:body></w:document>";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            entry(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package"
                    + ".relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd."
                    + "openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            entry(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/"
                    + "2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
            entry(zip, "word/document.xml", document);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String paragraph(String text, int halfPoints, boolean bold, boolean bullet) {
        String indent = bullet ? "<w:pPr><w:ind w:left=\"360\"/></w:pPr>" : "";
        String run = "<w:rPr>" + (bold ? "<w:b/>" : "") + "<w:sz w:val=\"" + halfPoints + "\"/></w:rPr>";
        return "<w:p>" + indent + "<w:r>" + run + "<w:t xml:space=\"preserve\">"
                + escape((bullet ? "• " : "") + text) + "</w:t></w:r></w:p>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
