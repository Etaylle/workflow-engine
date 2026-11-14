package engine.projects;

import java.util.List;

/** Offline writer for tests and ENGINE_AI_STUB=true: always the canteen example. */
public class StubBriefWriter implements BriefWriter {

    @Override
    public Brief write(String apiKey, String wish, String correction) {
        return new Brief(
                "Let students order a meal in advance and pick it up without queuing.",
                List.of("Student", "Kitchen"),
                List.of("Meal", "Order", "Pick-up slot"),
                List.of("Peak load at lunchtime", "Payment handling is not described"));
    }
}
