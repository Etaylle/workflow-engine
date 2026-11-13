package engine.settings.application.ai;

import java.util.List;

/**
 * Output of the DEVELOPMENT SUPERVISOR at the end of a run (workflow.md 7.5): causes that repeated across tasks
 * or runs, each with the cheapest automatic check that would catch it. The code keeps only causes with
 * occurrences >= 2 and writes settings.lessons (status PROPOSED); the client applies or dismisses them on page 7.
 */
public record LessonProposalOutput(List<LessonDraft> lessons) {

    public record LessonDraft(String cause, int occurrences, int runCount, String proposal, String targetStepKey) {
    }
}
