package engine.execution;

import java.util.regex.Pattern;

/**
 * Tells from the output of a failed builder run whether the rate limit or the credit of the key
 * is exhausted (US-06.10). Such a failure is not a defect of the code: the run waits and retries.
 */
final class LimitDetector {

    /** Subtypes of StepOutcome that mean "wait for tokens", not "fix the code". */
    static final java.util.Set<String> SUBTYPES = java.util.Set.of("rate_limit", "credit", "hard_stop");

    private static final Pattern CREDIT = Pattern.compile(
            "credit balance|insufficient credit|out of credit|billing|payment required",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RATE = Pattern.compile(
            "rate[ _-]?limit|\\b429\\b|usage limit|hit your limit|too many requests|overloaded|\\b529\\b",
            Pattern.CASE_INSENSITIVE);

    private LimitDetector() {
    }

    /** The subtype for the output ("credit", "rate_limit"), or null when it names no such limit. */
    static String classify(String output) {
        if (CREDIT.matcher(output).find()) {
            return "credit";
        }
        return RATE.matcher(output).find() ? "rate_limit" : null;
    }
}
