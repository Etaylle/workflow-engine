package engine.documents;

import engine.shared.ApiProblem;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operations getDocument and downloadDocument of the API. */
@RestController
class DocumentController {

    private static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocumentService service;

    DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{id}/documents/{kind}")
    DocumentModel get(@PathVariable UUID id, @PathVariable String kind) {
        return service.build(id, kind);
    }

    @GetMapping("/api/projects/{id}/documents/{kind}/download")
    ResponseEntity<byte[]> download(@PathVariable UUID id, @PathVariable String kind,
            @RequestParam(defaultValue = "docx") String format) {
        DocumentModel doc = service.build(id, kind);
        byte[] bytes;
        MediaType type;
        switch (format) {
            case "docx" -> {
                bytes = DocumentRenderer.docx(doc);
                type = DOCX;
            }
            case "md" -> {
                bytes = DocumentRenderer.markdown(doc).getBytes(StandardCharsets.UTF_8);
                type = MediaType.parseMediaType("text/markdown;charset=UTF-8");
            }
            default -> throw new ApiProblem(400, "Format unknown",
                    "The format is docx or md, not '" + format + "'.");
        }
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(kind + "." + format).build().toString())
                .body(bytes);
    }
}
