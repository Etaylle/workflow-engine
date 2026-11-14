package engine.documents;

import java.util.List;

/** Offline writer for tests and ENGINE_AI_STUB=true: always the canteen example. */
public class StubRequirementWriter implements RequirementWriter {

    @Override
    public RequirementDraft write(String apiKey, String wish, String material, String correction) {
        return new RequirementDraft(List.of(
                new RequirementDraft.Epic("Ordering", "Order", List.of(
                        new RequirementDraft.Item("functional", "must",
                                "A student can order a meal from the menu.", "wish"),
                        new RequirementDraft.Item("functional", "should",
                                "A student pays online when ordering.", "answer: payment"),
                        new RequirementDraft.Item("quality", "should",
                                "An order is confirmed within 3 seconds.", "wish"))),
                new RequirementDraft.Epic("Kitchen", "Kitchen", List.of(
                        new RequirementDraft.Item("functional", "must",
                                "The kitchen sees every open order and marks it ready.", "wish"),
                        new RequirementDraft.Item("constraint", "must",
                                "A meal can be picked up within the pick-up window.", "answer: window")))));
    }
}
