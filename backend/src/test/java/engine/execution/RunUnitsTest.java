package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import engine.shared.ContractPort.ContractInfo;
import engine.shared.ContractPort.TaskLine;
import engine.shared.Harness;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The units of work a run is cut into, and the effort a step runs with. */
class RunUnitsTest {

    private static ContractInfo contract(int softwareTasks) {
        return new ContractInfo(1, Optional.empty(), "# Contract",
                IntStream.rangeClosed(1, softwareTasks)
                        .mapToObj(n -> new TaskLine("Task " + n, "System", "Does " + n)).toList(),
                List.of(new TaskLine("Aim", "Player", "The player aims")));
    }

    @Test
    void taskByTaskFromThreeTasksStartsWithAWorkingWholeAndEndsWithAPassAgainstTheWish() {
        List<TaskLine> units = RunService.units(contract(3), "tasks");

        assertThat(units).extracting(TaskLine::title).containsExactly(RunService.FOUNDATION_TITLE,
                "Task 1", "Task 2", "Task 3", RunService.FINAL_TITLE);
        assertThat(units.get(0).description()).contains("plainest working form")
                .contains("Task 1; Task 2; Task 3").contains("Aim").contains("start to end");
        assertThat(units.get(4).description()).contains("ORIGINAL WISH").contains("critical client");
    }

    @Test
    void fewerThanThreeTasksStandAloneBecauseTheFrameWouldDoubleTheRun() {
        assertThat(RunService.units(contract(2), "tasks")).extracting(TaskLine::title)
                .containsExactly("Task 1", "Task 2");
        assertThat(RunService.units(contract(1), "tasks")).hasSize(1);
    }

    @Test
    void wholeModeIsOneUnitWhateverTheNumberOfTasks() {
        assertThat(RunService.units(contract(12), "whole")).extracting(TaskLine::title)
                .containsExactly(RunService.WHOLE_TITLE);
    }

    @Test
    void aStepWithoutAnEffortRunsWithTheEffortOfTheProject() {
        assertThat(RunOrchestrator.effortOf(Harness.CLAUDE_CODE, null, "max")).isEqualTo("max");
        assertThat(RunOrchestrator.effortOf(Harness.CODEX_CLI, null, null)).isNull();
    }

    @Test
    void theEffortOfAStepIsGivenInTheLevelsTheBuilderKnows() {
        assertThat(RunOrchestrator.effortOf(Harness.CLAUDE_CODE, "medium", "max")).isEqualTo("medium");
        assertThat(RunOrchestrator.effortOf(Harness.CODEX_CLI, "max", "high")).as("Codex has no max")
                .isEqualTo("xhigh");
        assertThat(RunOrchestrator.effortOf(Harness.CODEX_CLI, "low", "high")).isEqualTo("low");
        assertThat(RunOrchestrator.effortOf(Harness.GEMINI_CLI, "low", null)).as("Gemini has no effort")
                .isNull();
    }
}
