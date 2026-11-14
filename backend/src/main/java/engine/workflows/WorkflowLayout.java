package engine.workflows;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Swimlane layout: x by distance from the start, y by lane; nodes sharing a cell are stacked. */
final class WorkflowLayout {

    static final int COLUMN_WIDTH = 260;
    static final int LANE_HEIGHT = 180;
    static final int STACK_STEP = 70;

    private WorkflowLayout() {
    }

    /** Steps from the start to every node reachable from it (breadth first; cycles are safe). */
    static Map<String, Integer> distances(String start, Map<String, List<String>> out) {
        Map<String, Integer> distance = new HashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        distance.put(start, 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            String key = queue.poll();
            for (String next : out.getOrDefault(key, List.of())) {
                if (distance.putIfAbsent(next, distance.get(key) + 1) == null) {
                    queue.add(next);
                }
            }
        }
        return distance;
    }

    /** Position {x, y} per node key, for a validated draft. */
    static Map<String, int[]> positions(WorkflowDraft draft) {
        Map<String, List<String>> out = new HashMap<>();
        for (WorkflowDraft.Edge edge : draft.edges()) {
            out.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge.to());
        }
        String start =
                draft.nodes().stream().filter(n -> "start_event".equals(n.kind())).findFirst().orElseThrow().key();
        Map<String, Integer> column = distances(start, out);
        Map<String, Integer> stacked = new HashMap<>();
        Map<String, int[]> result = new HashMap<>();
        for (WorkflowDraft.Node node : draft.nodes()) {
            int x = column.get(node.key());
            int lane = draft.lanes().indexOf(node.lane());
            int slot = stacked.merge(lane + ":" + x, 1, Integer::sum) - 1;
            result.put(node.key(), new int[] {x * COLUMN_WIDTH, lane * LANE_HEIGHT + slot * STACK_STEP});
        }
        return result;
    }
}
