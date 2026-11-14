package engine.workflows;

import engine.shared.InvalidAiOutputException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class WorkflowValidatorTest {

    private static WorkflowDraft canteen() {
        return StubWorkflowWriter.canteen();
    }

    private static WorkflowDraft withNodes(UnaryOperator<List<WorkflowDraft.Node>> change) {
        WorkflowDraft d = canteen();
        return new WorkflowDraft(d.lanes(), change.apply(new ArrayList<>(d.nodes())), d.edges());
    }

    private static WorkflowDraft withEdges(UnaryOperator<List<WorkflowDraft.Edge>> change) {
        WorkflowDraft d = canteen();
        return new WorkflowDraft(d.lanes(), d.nodes(), change.apply(new ArrayList<>(d.edges())));
    }

    @Test
    void canteenExampleIsValid() {
        assertThatCode(() -> WorkflowValidator.validate(canteen())).doesNotThrowAnyException();
    }

    @Test
    void emptyDraftIsRejected() {
        assertThatThrownBy(() -> WorkflowValidator.validate(new WorkflowDraft(List.of(), List.of(), List.of())))
                .isInstanceOf(InvalidAiOutputException.class);
        assertThatThrownBy(() -> WorkflowValidator.validate(null)).isInstanceOf(InvalidAiOutputException.class);
    }

    @Test
    void unknownLaneIsRejected() {
        WorkflowDraft draft = withNodes(n -> {
            n.set(1, new WorkflowDraft.Node("order", "Ghost", "task", "Order", "", "software"));
            return n;
        });
        assertThatThrownBy(() -> WorkflowValidator.validate(draft)).hasMessageContaining("unknown lane");
    }

    @Test
    void taskWithoutTypeIsRejected() {
        WorkflowDraft draft = withNodes(n -> {
            n.set(1, new WorkflowDraft.Node("order", "Student", "task", "Order", "", ""));
            return n;
        });
        assertThatThrownBy(() -> WorkflowValidator.validate(draft)).hasMessageContaining("taskType");
    }

    @Test
    void edgeToUnknownNodeIsRejected() {
        WorkflowDraft draft = withEdges(e -> {
            e.add(new WorkflowDraft.Edge("end", "nowhere"));
            return e;
        });
        assertThatThrownBy(() -> WorkflowValidator.validate(draft)).hasMessageContaining("unknown node");
    }

    @Test
    void unreachableNodeIsRejected() {
        WorkflowDraft draft = withNodes(n -> {
            n.add(new WorkflowDraft.Node("lost", "Kitchen", "end_event", "Lost", "", ""));
            return n;
        });
        assertThatThrownBy(() -> WorkflowValidator.validate(draft)).hasMessageContaining("reachable");
    }

    @Test
    void deadEndTaskIsRejected() {
        WorkflowDraft draft = withEdges(e -> {
            e.removeIf(edge -> edge.from().equals("pickup"));
            return e;
        });
        assertThatThrownBy(() -> WorkflowValidator.validate(draft)).hasMessageContaining("no outgoing");
    }

    @Test
    void layoutPutsColumnsByStepAndRowsByLane() {
        Map<String, int[]> positions = WorkflowLayout.positions(canteen());

        assertThat(positions.get("start")).containsExactly(0, 0);
        assertThat(positions.get("cook")).containsExactly(2 * WorkflowLayout.COLUMN_WIDTH, WorkflowLayout.LANE_HEIGHT);
        assertThat(positions.get("end")[0]).isEqualTo(5 * WorkflowLayout.COLUMN_WIDTH);
    }
}
