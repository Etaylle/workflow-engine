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
                .containsExactly("java-svelte-postgres", "node-console", "sveltekit-sqlite");
        assertThat(catalog.require("node-console").isConsole()).isTrue();
        assertThat(catalog.require("node-console").needsDatabase()).isFalse();
        assertThat(catalog.require("java-svelte-postgres").needsDatabase()).isTrue();
        assertThat(catalog.require("sveltekit-sqlite").kind()).isEqualTo(Template.WEB);
    }

    @Test
    void defaultsFollowTheKind() {
        assertThat(catalog.defaultFor("console")).isEqualTo("node-console");
        assertThat(catalog.defaultFor("web")).isEqualTo("sveltekit-sqlite");
    }

    @Test
    void anUnknownTemplateIsRefusedWithTheKnownIds() {
        assertThatThrownBy(() -> catalog.require("cobol-mainframe"))
                .isInstanceOf(ApiProblem.class).hasMessageContaining("node-console");
    }
}
