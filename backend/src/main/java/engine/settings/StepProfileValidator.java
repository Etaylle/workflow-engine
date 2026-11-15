package engine.settings;

import engine.shared.ApiProblem;
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
    static final BigDecimal MAX_CAP = BigDecimal.TEN;
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
        }
        return raw.stream().map(s -> new Step(s.key(), s.title().strip(), s.gated(), s.capUsd(),
                s.prompt().strip())).toList();
    }

    private static void checkText(String where, Step step) {
        if (step.title() == null || step.title().isBlank() || step.title().strip().length() > MAX_TITLE) {
            throw bad(where + "the title needs 1 to " + MAX_TITLE + " characters.");
        }
        if (step.prompt() == null || step.prompt().isBlank() || step.prompt().strip().length() > MAX_PROMPT) {
            throw bad(where + "the prompt needs 1 to " + MAX_PROMPT + " characters.");
        }
        if (step.capUsd() == null || step.capUsd().signum() <= 0 || step.capUsd().compareTo(MAX_CAP) > 0) {
            throw bad(where + "the cost cap must be above 0 and at most USD " + MAX_CAP + ".");
        }
    }

    private static ApiProblem bad(String detail) {
        return new ApiProblem(400, "Invalid step profile", detail);
    }
}
