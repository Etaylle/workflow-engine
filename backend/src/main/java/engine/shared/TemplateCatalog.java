package engine.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The templates the engine offers: every folder of the templates directory with a template.json. */
@Component
public class TemplateCatalog {

    /** Template of a web wish when the client does not choose another. */
    public static final String DEFAULT_WEB = "sveltekit-sqlite";
    /** Template of a console wish. */
    public static final String DEFAULT_CONSOLE = "node-console";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Path root;

    TemplateCatalog(@Value("${engine.execution.templates-dir:templates}") String root) {
        this.root = Path.of(root).toAbsolutePath();
    }

    /** All templates, sorted by id; a folder with a broken manifest fails loudly. */
    public List<Template> list() {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("The templates folder is missing: " + root
                    + " (set ENGINE_EXECUTION_TEMPLATES_DIR)");
        }
        try (Stream<Path> dirs = Files.list(root)) {
            return dirs.filter(dir -> Files.isRegularFile(dir.resolve("template.json"))).sorted()
                    .map(TemplateCatalog::read).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list the templates in " + root, e);
        }
    }

    /** The template with this id, or empty. */
    public Optional<Template> find(String id) {
        return list().stream().filter(template -> template.id().equals(id)).findFirst();
    }

    /** The template with this id, or a 400 problem naming the known ids. */
    public Template require(String id) {
        return find(id).orElseThrow(() -> new ApiProblem(400, "Unknown template",
                "Template '" + id + "' is not one of " + list().stream().map(Template::id).toList()));
    }

    /** Template in which the builder chooses the technology; the default when it is offered. */
    public static final String FREE = "free";

    /**
     * The default template of a kind of software (console or web): the free template when the
     * engine offers it, otherwise the prepared template of the kind.
     */
    public String defaultFor(String kind) {
        if (Files.isRegularFile(root.resolve(FREE).resolve("template.json"))) {
            return FREE;
        }
        return Template.CONSOLE.equals(kind) ? DEFAULT_CONSOLE : DEFAULT_WEB;
    }

    private static Template read(Path dir) {
        try {
            JsonNode node = JSON.readTree(Files.readString(dir.resolve("template.json")));
            String kind = node.path("kind").asString();
            if (!List.of(Template.CONSOLE, Template.WEB, Template.ANY).contains(kind)) {
                throw new IllegalStateException("kind must be console, web or any");
            }
            return new Template(dir.getFileName().toString(), node.path("name").asString(), kind,
                    node.path("description").asString(), node.path("needsDatabase").asBoolean(false), dir);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Bad template.json in " + dir + ": " + e.getMessage(), e);
        }
    }
}
