package engine.workflows.application.ai;

import engine.workflows.domain.NodeKind;
import engine.workflows.domain.TaskType;
import java.util.List;
import java.util.UUID;

/**
 * Tools of the WORKFLOW AGENT (FR-AGT-07). The adapter exposes these methods with Spring AI @Tool
 * (and, optionally, through the MCP server at /mcp). Each call writes through the workflows use cases,
 * so the same validation as the REST API applies.
 *   addLane     -> workflows.lanes
 *   addEvent    -> workflows.nodes (kind START_EVENT / END_EVENT)
 *   addTask     -> workflows.nodes (kind TASK) + workflows.acceptance_criteria + workflows.node_requirements
 *   connect     -> workflows.edges
 *   validate    -> no write; returns the problems (one start, one end, all reachable, no cycle)
 */
public interface WorkflowTools {

    UUID addLane(UUID workflowId, String name);

    UUID addEvent(UUID workflowId, UUID laneId, NodeKind kind);

    UUID addTask(UUID workflowId, UUID laneId, String title, String description, TaskType taskType,
            String epicKey, String storyKey, List<String> requirementKeys, List<String> acceptanceCriteria);

    UUID connect(UUID workflowId, UUID sourceNodeId, UUID targetNodeId);

    List<String> validate(UUID workflowId);
}
