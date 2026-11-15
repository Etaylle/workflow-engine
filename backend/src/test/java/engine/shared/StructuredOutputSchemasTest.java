package engine.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.core.ObjectMappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import engine.documents.RequirementDraft;
import engine.projects.Brief;
import engine.questions.QuestionDraft;
import engine.workflows.WorkflowDraft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The schemas the SDK derives for the four planning answers, checked against the documented
 * limits of structured outputs: every object closed (additionalProperties false) with all fields
 * required, no numeric, string or array-size constraints, no recursion, shallow nesting.
 */
class StructuredOutputSchemasTest {

    private static final ObjectMapper JSON = ObjectMappers.jsonMapper();
    private static final Set<String> UNSUPPORTED =
            Set.of("minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
                    "minLength", "maxLength", "pattern", "maxItems", "minContains", "maxContains",
                    "not", "if", "then", "else", "patternProperties", "allOf", "oneOf");
    private static final int MAX_DEPTH = 5;

    static Stream<Class<?>> answerClasses() {
        return Stream.of(Brief.class, QuestionDraft.class, RequirementDraft.class, WorkflowDraft.class);
    }

    private static JsonNode schemaOf(Class<?> type) {
        var format = AnthropicCall.params("s", "u", type).outputConfig().orElseThrow().format().orElseThrow();
        return JSON.valueToTree(format.schema()._additionalProperties());
    }

    /** The schema the OpenAI SDK derives for the strict response format (OpenAI, Gemini, ...). */
    private static JsonNode openAiSchemaOf(Class<?> type) throws com.fasterxml.jackson.core.JsonProcessingException {
        var target = new AiTarget(AiProvider.OPENAI, "k", "https://api.openai.com/v1", "m");
        var strict = OpenAiCall.params(target, "s", "u", type);
        assertThat(strict.rawParams().responseFormat().orElseThrow().asJsonSchema().jsonSchema().strict())
                .contains(true);
        return JSON.readTree(OpenAiCall.schemaOf(strict));
    }

    @ParameterizedTest
    @MethodSource("answerClasses")
    void schemaMeetsTheStrictRulesOfOpenAiAndGemini(Class<?> type) throws Exception {
        JsonNode schema = openAiSchemaOf(type);

        List<String> problems = new ArrayList<>();
        walk(schema, schema, "$", 0, problems);
        assertThat(problems).isEmpty();
        assertThat(schema.path("type").asText()).isEqualTo("object");
        assertThat(schema.toString()).doesNotContain("\"format\"", "\"default\"");
        assertThat(countProperties(schema)).as("OpenAI allows 5000 properties").isLessThan(5000);
    }

    @ParameterizedTest
    @MethodSource("answerClasses")
    void bothProvidersGetTheSameFields(Class<?> type) throws Exception {
        assertThat(names(openAiSchemaOf(type).path("properties")))
                .isEqualTo(names(schemaOf(type).path("properties")));
    }

    private static int countProperties(JsonNode node) {
        int[] total = {0};
        node.findParents("type").forEach(n -> total[0] += n.path("properties").size());
        return total[0];
    }

    @ParameterizedTest
    @MethodSource("answerClasses")
    void schemaIsAcceptedByTheSdkAndClosed(Class<?> type) {
        JsonNode schema = schemaOf(type);

        assertThat(schema.path("type").asText()).isEqualTo("object");
        List<String> problems = new ArrayList<>();
        walk(schema, schema, "$", 0, problems);
        assertThat(problems).isEmpty();
    }

    @Test
    void schemaListsTheFieldsClaudeMustWrite() {
        assertThat(names(schemaOf(Brief.class).path("properties")))
                .containsExactlyInAnyOrder("goal", "actors", "domainTerms", "risks", "softwareKind");
        assertThat(schemaOf(WorkflowDraft.class).path("required").toString())
                .contains("lanes", "nodes", "edges");
    }

    private static Set<String> names(JsonNode object) {
        Set<String> names = new HashSet<>();
        object.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static void walk(JsonNode root, JsonNode node, String path, int depth, List<String> problems) {
        if (depth > MAX_DEPTH) {
            problems.add(path + " is nested deeper than " + MAX_DEPTH);
            return;
        }
        UNSUPPORTED.stream().filter(node::has).forEach(k -> problems.add(path + " uses " + k));
        if (node.has("minItems") && node.get("minItems").asInt() > 1) {
            problems.add(path + " has minItems above 1");
        }
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            JsonNode target = root.at(ref.substring(1));
            if (target.isMissingNode()) {
                problems.add(path + " points to the missing " + ref);
            } else if (depth > 0 && path.contains(ref)) {
                problems.add(path + " is recursive via " + ref);
            } else {
                walk(root, target, path + "->" + ref, depth + 1, problems);
            }
        }
        if ("object".equals(node.path("type").asText())) {
            if (!node.path("additionalProperties").isBoolean() || node.get("additionalProperties").asBoolean()) {
                problems.add(path + " is not closed (additionalProperties must be false)");
            }
            Set<String> required = new HashSet<>();
            node.path("required").forEach(r -> required.add(r.asText()));
            if (!required.equals(names(node.path("properties")))) {
                problems.add(path + " does not require all its properties");
            }
        }
        node.path("properties").properties().forEach(
                e -> walk(root, e.getValue(), path + "." + e.getKey(), depth + 1, problems));
        if (node.has("items")) {
            walk(root, node.get("items"), path + "[]", depth + 1, problems);
        }
        node.path("anyOf").forEach(n -> walk(root, n, path + "|", depth + 1, problems));
        node.path("$defs").properties().forEach(
                e -> walk(root, e.getValue(), path + "$defs." + e.getKey(), depth + 1, problems));
    }
}
