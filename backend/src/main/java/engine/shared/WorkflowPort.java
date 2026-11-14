package engine.shared;

import java.util.List;
import java.util.UUID;

/** What other domains may read of the domain "workflows". */
public interface WorkflowPort {

    /** One step of the workflow: lane (actor), kind, title, description and task type if a task. */
    record StepLine(String lane, String kind, String title, String description, String taskType) {
    }

    /** The steps in the order of the flow (left to right); empty when there is no workflow. */
    List<StepLine> stepsOf(UUID projectId);
}
