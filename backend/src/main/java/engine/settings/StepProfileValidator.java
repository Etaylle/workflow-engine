package engine.settings;

import engine.shared.ApiProblem;
import engine.shared.BuilderChoice;
import engine.shared.RunSettingsPort.Step;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** The rules of an edited profile; every break is a 400 that says which step and what to change. */
final class StepProfileValidator {

    static final int MAX_STEPS = 20;
    static final int MAX_NAME = 60;
    static final int MAX_TITLE = 80;
    static final int MAX_PROMPT = 4000;
    static final BigDecimal MAX_CAP = BigDecimal.valueOf(1000);
    private static final Pattern KEY = Pattern.compile("^[a-z0-9-]{1,40}$");

    private StepProfileValidator() {
    }

    static String name(String raw) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty() || name.length() > MAX_NAME) {
            throw bad("Give the profile a name of 1 to " + MAX_NAME + " characters.");
        }
        return name;
    }

    static List<Step> steps(List<Step> raw) {
        if (raw == null || raw.isEmpty() || raw.size() > MAX_STEPS) {
            throw bad("A profile has 1 to " + MAX_STEPS + " steps.");
        }
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < raw.size(); i++) {
            Step step = raw.get(i);
            String where = "Step " + (i + 1) + ": ";
            if (step == null || step.key() == null || !KEY.matcher(step.key()).matches()) {
                throw bad(where + "the key must be 1 to 40 characters of a-z, 0-9 and -.");
            }
            if (!keys.add(step.key())) {
                throw bad(where + "the key " + step.key() + " is used twice.");
            }
            checkText(where, step);
            if (step.reviewerOnly() && !step.review()) {
                throw bad(where + "only an independent review step can be limited to runs with a reviewer."
                        + " Mark it as a review step or remove the limit.");
            }
        }
        if (raw.stream().allMatch(Step::reviewerOnly)) {
            throw bad("Every step runs only with a reviewer, so a run without one would have no steps."
                    + " Leave at least one step that always runs.");
        }
        return raw.stream().map(s -> new Step(s.key(), s.title().strip(), s.gated(), s.capUsd(),
                s.prompt().strip(), s.review(), s.reviewerOnly(), effort(s.effort()))).toList();
    }

    private static void checkText(String where, Step step) {
        if (step.title() == null || step.title().isBlank() || step.title().strip().length() > MAX_TITLE) {
            throw bad(where + "the title needs 1 to " + MAX_TITLE + " characters.");
        }
        if (step.prompt() == null || step.prompt().isBlank() || step.prompt().strip().length() > MAX_PROMPT) {
            throw bad(where + "the prompt needs 1 to " + MAX_PROMPT + " characters.");
        }
        if (step.capUsd() != null && (step.capUsd().signum() <= 0 || step.capUsd().compareTo(MAX_CAP) > 0)) {
            throw bad(where + "the cost cap must be above 0 and at most USD " + MAX_CAP
                    + ", or left empty for no cap.");
        }
        String effort = effort(step.effort());
        if (effort != null && !BuilderChoice.EFFORTS.contains(effort)) {
            throw bad(where + "the effort must be one of " + String.join(", ", BuilderChoice.EFFORTS)
                    + ", or left empty for the effort of the project.");
        }
    }

    private static String effort(String raw) {
        return raw == null || raw.isBlank() ? null : raw.strip();
    }

    private static ApiProblem bad(String detail) {
        return new ApiProblem(400, "Invalid step profile", detail);
    }
}
