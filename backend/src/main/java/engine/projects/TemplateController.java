package engine.projects;

import engine.shared.Template;
import engine.shared.TemplateCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lists the project templates the engine offers. */
@RestController
class TemplateController {

    /** One template as the API shows it. */
    record TemplateView(String id, String name, String kind, String description) { }

    private final TemplateCatalog catalog;

    TemplateController(TemplateCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/templates")
    List<TemplateView> list() {
        return catalog.list().stream()
                .map((Template t) -> new TemplateView(t.id(), t.name(), t.kind(), t.description())).toList();
    }
}
