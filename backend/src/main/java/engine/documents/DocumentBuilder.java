package engine.documents;

import engine.documents.DocumentModel.Section;
import engine.shared.AnswersPort.AnswerLine;
import engine.shared.RequirementsPort.RequirementLine;
import engine.shared.WorkflowPort.StepLine;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the two project documents from the planning material; no AI call is needed. */
final class DocumentBuilder {

    private DocumentBuilder() {
    }

    /** What the documents are made of. */
    record Material(String name, String wish, String brief, List<AnswerLine> answers,
            List<RequirementLine> requirements, List<StepLine> steps) {
    }

    static DocumentModel requirements(Material m) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section("Wish", List.of(m.wish()), List.of()));
        if (m.brief() != null) {
            sections.add(new Section("Brief", m.brief().lines().toList(), List.of()));
        }
        sections.add(new Section("Decisions of the client", List.of(), m.answers().stream()
                .map(a -> a.question() + " " + a.answer() + " (" + decidedBy(a) + ")").toList()));
        Map<String, List<String>> byEpic = new LinkedHashMap<>();
        for (RequirementLine r : m.requirements()) {
            byEpic.computeIfAbsent(r.epic(), k -> new ArrayList<>())
                    .add(r.key() + " [" + r.priority() + "] " + r.text());
        }
        byEpic.forEach((epic, lines) -> sections.add(new Section("Epic " + epic, List.of(), lines)));
        return new DocumentModel("Requirements: " + m.name(), sections);
    }

    static DocumentModel plan(Material m) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section("Goal and scope", m.brief() == null ? List.of(m.wish())
                : m.brief().lines().toList(), List.of()));
        List<String> flow = new ArrayList<>();
        int number = 1;
        for (StepLine step : m.steps()) {
            flow.add(number++ + ". " + step.title() + " (" + step.lane() + ", " + kind(step) + ")"
                    + (step.description() == null ? "" : ": " + step.description()));
        }
        sections.add(new Section("Workflow in order", List.of(), flow));
        List<String> software = m.steps().stream().filter(s -> "software".equals(s.taskType()))
                .map(StepLine::title).toList();
        sections.add(new Section("Tasks to build", List.of(software.size() + " software tasks are"
                + " built one after the other in this order; the manual tasks stay with people."),
                software));
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String priority : List.of("must", "should", "could", "wont")) {
            counts.put(priority, m.requirements().stream().filter(r -> priority.equals(r.priority())).count());
        }
        sections.add(new Section("Requirements by priority", List.of(), counts.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue()).toList()));
        return new DocumentModel("Project plan: " + m.name(), sections);
    }

    private static String decidedBy(AnswerLine answer) {
        return "ai".equals(answer.decidedBy()) ? "AI proposal" : "decided by the client";
    }

    private static String kind(StepLine step) {
        return switch (step.kind()) {
            case "start_event" -> "start";
            case "end_event" -> "end";
            default -> step.taskType();
        };
    }
}
