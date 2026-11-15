package engine.workflows;

import engine.shared.ContractPort.TaskLine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Writes a frozen contract snapshot as the Markdown text the builder reads (CONTRACT.md). */
final class ContractMarkdown {

    private ContractMarkdown() {
    }

    static String of(int version, WorkflowStore.Snapshot snapshot) {
        StringBuilder text = new StringBuilder("# Confirmed contract (version " + version + ")\n\n")
                .append("It never changes. The builder must satisfy it.\n\n## Wish\n\n")
                .append(snapshot.wish()).append("\n\n## Answers to the questions\n\n");
        snapshot.answers().forEach(a -> text.append("- ").append(a.question()).append(" ")
                .append(a.answer()).append("\n"));
        text.append("\n## Requirements\n\n");
        snapshot.requirements().forEach(r -> text.append("- ").append(r.key()).append(" (")
                .append(r.priority()).append(", ").append(r.epic()).append("): ")
                .append(r.text()).append("\n"));
        text.append("\n## Software template\n\n").append(snapshot.template() == null ? "(not chosen)"
                : snapshot.template()).append("\n");
        text.append("\n## Workflow\n\n");
        Map<UUID, String> lanes = laneNames(snapshot.workflow());
        snapshot.workflow().nodes().forEach(n -> text.append("- ").append(n.kind()).append(" \"")
                .append(n.title()).append("\" (").append(lanes.get(n.laneId()))
                .append(n.taskType() == null ? "" : ", " + n.taskType()).append(")")
                .append(n.description() == null || n.description().isBlank() ? ""
                        : ": " + n.description())
                .append("\n"));
        return text.toString();
    }

    static List<TaskLine> softwareTasks(WorkflowView workflow) {
        Map<UUID, String> lanes = laneNames(workflow);
        return workflow.nodes().stream()
                .filter(n -> "task".equals(n.kind()) && "software".equals(n.taskType()))
                .map(n -> new TaskLine(n.title(), lanes.get(n.laneId()), n.description()))
                .toList();
    }

    private static Map<UUID, String> laneNames(WorkflowView workflow) {
        Map<UUID, String> lanes = new HashMap<>();
        workflow.lanes().forEach(lane -> lanes.put(lane.id(), lane.name()));
        return lanes;
    }
}
