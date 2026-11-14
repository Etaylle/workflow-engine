package engine.workflows;

import java.util.List;
import java.util.UUID;

/** The workflow as the API shows it; positions come from the automatic swimlane layout. */
public record WorkflowView(
        UUID id,
        UUID projectId,
        String status,
        int version,
        Integer confirmedContractVersion,
        List<Lane> lanes,
        List<Node> nodes,
        List<Edge> edges) {

    public record Lane(UUID id, String name) {
    }

    public record Node(
            UUID id,
            UUID laneId,
            String kind,
            String title,
            String description,
            String taskType,
            int posX,
            int posY) {
    }

    public record Edge(UUID id, UUID source, UUID target) {
    }
}
