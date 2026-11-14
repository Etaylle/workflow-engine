package engine.questions;

import engine.shared.AnswersPort;
import engine.shared.ApiProblem;
import engine.shared.ClaudeCall;
import engine.shared.ProjectPort;
import engine.shared.SessionKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Asks, stores and takes the answers of the questions of a project. */
@Service
class QuestionService implements AnswersPort {

    /** One answer of the request: the proposal accepted, or the client's own value. */
    record AnswerInput(UUID questionId, Boolean acceptProposal, JsonNode value) {
    }

    /** Body of saveAnswers. */
    record SaveAnswersRequest(List<AnswerInput> answers) {
    }

    private static final int MAX_TEXT = 2_000;

    private final QuestionWriter writer;
    private final QuestionStore store;
    private final ProjectPort projects;
    private final SessionKey sessionKey;
    private final JsonMapper json;

    QuestionService(QuestionWriter writer, QuestionStore store, ProjectPort projects,
            SessionKey sessionKey, JsonMapper json) {
        this.writer = writer;
        this.store = store;
        this.projects = projects;
        this.sessionKey = sessionKey;
        this.json = json;
    }

    /** One planning call (one retry); replaces earlier questions while nothing is built on them. */
    List<QuestionView> generate(UUID projectId) {
        String wish = projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        String status = projects.statusOf(projectId).orElseThrow(() -> notFound(projectId));
        if (!Set.of("draft", "questions_ready").contains(status)) {
            throw new ApiProblem(409, "Questions are fixed", "The questions were already answered;"
                    + " the requirements are based on them.");
        }
        String brief = projects.briefOf(projectId).orElseThrow(() -> new ApiProblem(409,
                "Brief missing", "Write the brief first; the questions are based on it."));
        String key = ClaudeCall.requireKey(sessionKey);
        QuestionDraft draft = ClaudeCall.validated("questions",
                correction -> writer.write(key, wish, brief, correction), QuestionValidator::validate);
        store.replace(projectId, draft);
        projects.markStatus(projectId, "questions_ready");
        return store.find(projectId);
    }

    List<QuestionView> list(UUID projectId) {
        projects.wishOf(projectId).orElseThrow(() -> notFound(projectId));
        return store.find(projectId);
    }

    /** Saves exactly one answer for every question; accepting the proposal marks it as the AI's. */
    List<QuestionView> save(UUID projectId, SaveAnswersRequest request) {
        String status = projects.statusOf(projectId).orElseThrow(() -> notFound(projectId));
        if (!Set.of("questions_ready", "answered").contains(status)) {
            throw new ApiProblem(409, "Answers are not open", "There are no open questions, or the"
                    + " requirements are already written from the answers.");
        }
        List<QuestionView> questions = store.find(projectId);
        List<AnswerInput> inputs = request == null || request.answers() == null
                ? List.of() : request.answers();
        Map<UUID, QuestionView.Answer> answers = new HashMap<>();
        for (AnswerInput input : inputs) {
            QuestionView question = questions.stream()
                    .filter(q -> q.id().equals(input.questionId())).findFirst()
                    .orElseThrow(() -> new ApiProblem(400, "Unknown question",
                            "No question " + input.questionId() + " in this project."));
            if (answers.put(question.id(), resolve(question, input)) != null) {
                throw new ApiProblem(400, "Answered twice", "Question " + question.seq()
                        + " has two answers.");
            }
        }
        for (QuestionView question : questions) {
            if (!answers.containsKey(question.id())) {
                throw new ApiProblem(400, "Answer missing", "Question " + question.seq()
                        + " has no answer; answer it or accept the proposal.");
            }
        }
        store.saveAnswers(projectId, answers);
        projects.markStatus(projectId, "answered");
        return store.find(projectId);
    }

    private QuestionView.Answer resolve(QuestionView question, AnswerInput input) {
        if (Boolean.TRUE.equals(input.acceptProposal())) {
            return new QuestionView.Answer(question.proposal(), "ai");
        }
        JsonNode value = input.value();
        String where = "Question " + question.seq();
        if ("multiple_choice".equals(question.answerType())) {
            if (value == null || !value.isArray() || value.isEmpty()) {
                throw new ApiProblem(400, "Answer invalid", where + " needs at least one option.");
            }
            value.forEach(item -> requireOption(question, item, where));
        } else if ("single_choice".equals(question.answerType())) {
            requireOption(question, value, where);
        } else if (value == null || !value.isString() || value.asString().isBlank()
                || value.asString().length() > MAX_TEXT) {
            throw new ApiProblem(400, "Answer invalid",
                    where + " needs a text of 1 to " + MAX_TEXT + " characters.");
        }
        return new QuestionView.Answer(value, "client");
    }

    private static void requireOption(QuestionView question, JsonNode value, String where) {
        if (value == null || !value.isString() || !question.options().contains(value.asString())) {
            throw new ApiProblem(400, "Answer invalid", where + " needs one of its options.");
        }
    }

    @Override
    public List<AnswerLine> answersOf(UUID projectId) {
        return store.find(projectId).stream()
                .filter(q -> q.answer() != null)
                .map(q -> new AnswerLine(q.text(), text(q.answer().value()), q.answer().decidedBy()))
                .toList();
    }

    private static String text(JsonNode value) {
        if (value.isArray()) {
            return String.join(", ", value.valueStream().map(JsonNode::asString).toList());
        }
        return value.asString();
    }

    private static ApiProblem notFound(UUID id) {
        return new ApiProblem(404, "Project not found", "No project " + id);
    }
}
