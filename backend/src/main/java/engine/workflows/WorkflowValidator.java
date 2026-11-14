package engine.workflows;

import engine.shared.InvalidAiOutputException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checks a draft against the rules of the prompt; the first violation is reported. */
final class WorkflowValidator {

    static final int MAX_LANES = 8;
    static final int MAX_NODES = 40;
    static final Set<String> KINDS = Set.of("start_event", "task", "end_event");
    static final Set<String> TASK_TYPES = Set.of("software", "manual");

    private WorkflowValidator() {
    }

    static void validate(WorkflowDraft draft) {
        if (draft == null || draft.lanes() == null || draft.nodes() == null || draft.edges() == null) {
            throw new InvalidAiOutputException("lanes, nodes and edges are required.");
        }
        Set<String> lanes = checkLanes(draft.lanes());
        Map<String, WorkflowDraft.Node> nodes = checkNodes(draft.nodes(), lanes);
        Map<String, List<String>> out = checkEdges(draft.edges(), nodes);
        checkShape(nodes, out, draft.edges());
    }

    private static Set<String> checkLanes(List<String> lanes) {
        if (lanes.isEmpty() || lanes.size() > MAX_LANES) {
            throw new InvalidAiOutputException("Use 1 to " + MAX_LANES + " lanes.");
        }
        Set<String> names = new HashSet<>();
        for (String lane : lanes) {
            if (lane == null || lane.isBlank() || !names.add(lane)) {
                throw new InvalidAiOutputException("Lane names must be non-empty and unique.");
            }
        }
        return names;
    }

    private static Map<String, WorkflowDraft.Node> checkNodes(
            List<WorkflowDraft.Node> list, Set<String> lanes) {
        if (list.isEmpty() || list.size() > MAX_NODES) {
            throw new InvalidAiOutputException("Use 1 to " + MAX_NODES + " nodes.");
        }
        Map<String, WorkflowDraft.Node> nodes = new HashMap<>();
        for (WorkflowDraft.Node node : list) {
            if (node == null || node.key() == null || node.key().isBlank()
                    || nodes.put(node.key(), node) != null) {
                throw new InvalidAiOutputException("Node keys must be non-empty and unique.");
            }
            checkNode(node, lanes);
        }
        return nodes;
    }

    private static void checkNode(WorkflowDraft.Node node, Set<String> lanes) {
        String key = node.key();
        if (node.title() == null || node.title().isBlank()) {
            throw new InvalidAiOutputException("Node " + key + " has no title.");
        }
        if (!lanes.contains(node.lane())) {
            throw new InvalidAiOutputException("Node " + key + " names an unknown lane.");
        }
        if (!KINDS.contains(node.kind())) {
            throw new InvalidAiOutputException("Node " + key + " has an unknown kind.");
        }
        boolean task = "task".equals(node.kind());
        if (task && !TASK_TYPES.contains(node.taskType())) {
            throw new InvalidAiOutputException("Task " + key + " needs taskType software or manual.");
        }
    }

    private static Map<String, List<String>> checkEdges(
            List<WorkflowDraft.Edge> edges, Map<String, WorkflowDraft.Node> nodes) {
        Map<String, List<String>> out = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (WorkflowDraft.Edge edge : edges) {
            if (edge == null || !nodes.containsKey(edge.from()) || !nodes.containsKey(edge.to())) {
                throw new InvalidAiOutputException("An edge names an unknown node.");
            }
            if (edge.from().equals(edge.to()) || !seen.add(edge.from() + "->" + edge.to())) {
                throw new InvalidAiOutputException("Edges must not loop onto themselves or repeat.");
            }
            out.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge.to());
        }
        return out;
    }

    private static void checkShape(
            Map<String, WorkflowDraft.Node> nodes,
            Map<String, List<String>> out,
            List<WorkflowDraft.Edge> edges) {
        List<String> starts = keysOfKind(nodes, "start_event");
        if (starts.size() != 1) {
            throw new InvalidAiOutputException("Exactly one start_event is required.");
        }
        if (keysOfKind(nodes, "end_event").isEmpty()) {
            throw new InvalidAiOutputException("At least one end_event is required.");
        }
        if (edges.stream().anyMatch(e -> starts.contains(e.to()))) {
            throw new InvalidAiOutputException("The start_event must have no incoming edge.");
        }
        for (WorkflowDraft.Node node : nodes.values()) {
            boolean end = "end_event".equals(node.kind());
            if (!end && out.getOrDefault(node.key(), List.of()).isEmpty()) {
                throw new InvalidAiOutputException("Node " + node.key() + " has no outgoing edge.");
            }
            if (end && !out.getOrDefault(node.key(), List.of()).isEmpty()) {
                throw new InvalidAiOutputException("End event " + node.key() + " has an outgoing edge.");
            }
        }
        if (WorkflowLayout.distances(starts.get(0), out).size() != nodes.size()) {
            throw new InvalidAiOutputException("Every node must be reachable from the start_event.");
        }
    }

    private static List<String> keysOfKind(Map<String, WorkflowDraft.Node> nodes, String kind) {
        return nodes.values().stream().filter(n -> kind.equals(n.kind())).map(WorkflowDraft.Node::key).toList();
    }
}
