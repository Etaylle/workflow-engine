package engine.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The three real templates declare themselves with a template.json. */
class TemplateCatalogTest {

    private final TemplateCatalog catalog = new TemplateCatalog("../templates");

    @Test
    void listsTheRealTemplatesWithKindAndDatabaseNeed() {
        List<Template> all = catalog.list();

        assertThat(all).extracting(Template::id)
                .containsExactly("free", "java-svelte-postgres", "node-console", "sveltekit-sqlite");
        assertThat(catalog.require("free").isFree()).isTrue();
        assertThat(catalog.require("node-console").isConsole()).isTrue();
        assertThat(catalog.require("node-console").needsDatabase()).isFalse();
        assertThat(catalog.require("java-svelte-postgres").needsDatabase()).isTrue();
        assertThat(catalog.require("sveltekit-sqlite").kind()).isEqualTo(Template.WEB);
    }

    @Test
    void theFreeTemplateIsTheDefaultWhereItIsOffered() {
        assertThat(catalog.defaultFor("console")).isEqualTo("free");
        assertThat(catalog.defaultFor("web")).isEqualTo("free");
    }

    @Test
    void withoutTheFreeTemplateTheDefaultFollowsTheKind() {
        TemplateCatalog prepared = new TemplateCatalog("src/test/resources/fake-templates");

        assertThat(prepared.defaultFor("console")).isEqualTo("node-console");
        assertThat(prepared.defaultFor("web")).isEqualTo("sveltekit-sqlite");
    }

    @Test
    void aFreeTemplateTakesItsKindFromAppJsonOfTheProject(@org.junit.jupiter.api.io.TempDir java.nio.file.Path project)
            throws java.io.IOException {
        Template free = catalog.require("free");

        assertThat(free.kindIn(project)).as("no app.json").isEqualTo(Template.WEB);
        java.nio.file.Files.writeString(project.resolve("app.json"), "{ \"kind\" : \"console\" }");
        assertThat(free.kindIn(project)).isEqualTo(Template.CONSOLE);
        assertThat(catalog.require("sveltekit-sqlite").kindIn(project)).as("a prepared template keeps its kind")
                .isEqualTo(Template.WEB);
    }

    @Test
    void anUnknownTemplateIsRefusedWithTheKnownIds() {
        assertThatThrownBy(() -> catalog.require("cobol-mainframe"))
                .isInstanceOf(ApiProblem.class).hasMessageContaining("node-console");
    }
}
