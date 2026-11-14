package engine.workflows;

import java.util.List;

/** Offline writer for tests and ENGINE_AI_STUB=true: always the canteen example. */
public class StubWorkflowWriter implements WorkflowWriter {

    @Override
    public WorkflowDraft write(String apiKey, String wish, String material, String correction) {
        return canteen();
    }

    public static WorkflowDraft canteen() {
        return new WorkflowDraft(
                List.of("Student", "Kitchen"),
                List.of(
                        node("start", "Student", "start_event", "Hungry", "", ""),
                        node("order", "Student", "task", "Order a meal", "Pick a meal and pay.", "software"),
                        node("cook", "Kitchen", "task", "Prepare the meal", "Cook the ordered meal.", "manual"),
                        node("notify", "Kitchen", "task", "Notify the student", "Tell that the meal is ready.", "software"),
                        node("pickup", "Student", "task", "Pick up the meal", "", "manual"),
                        node("end", "Student", "end_event", "Meal served", "", "")),
                List.of(
                        edge("start", "order"),
                        edge("order", "cook"),
                        edge("cook", "notify"),
                        edge("notify", "pickup"),
                        edge("pickup", "end")));
    }

    private static WorkflowDraft.Node node(
            String key, String lane, String kind, String title, String description, String taskType) {
        return new WorkflowDraft.Node(key, lane, kind, title, description, taskType);
    }

    private static WorkflowDraft.Edge edge(String from, String to) {
        return new WorkflowDraft.Edge(from, to);
    }
}
