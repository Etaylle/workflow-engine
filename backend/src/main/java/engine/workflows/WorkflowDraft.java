package engine.workflows;

import java.util.List;

/**
 * The workflow as Claude writes it (structured output by class). Kinds are start_event, task and
 * end_event; taskType is software or manual for a task and empty otherwise.
 */
public record WorkflowDraft(List<String> lanes, List<Node> nodes, List<Edge> edges) {

    /** One start event, task or end event; key is only used by the edges. */
    public record Node(
            String key, String lane, String kind, String title, String description, String taskType) {
    }

    /** One sequence flow between two node keys. */
    public record Edge(String from, String to) {
    }
}
