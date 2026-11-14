package engine.questions;

import engine.questions.QuestionService.SaveAnswersRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operations generateQuestions, getQuestions and saveAnswers of the API. */
@RestController
class QuestionController {

    private final QuestionService service;

    QuestionController(QuestionService service) {
        this.service = service;
    }

    @PostMapping("/api/projects/{id}/questions")
    List<QuestionView> generate(@PathVariable UUID id) {
        return service.generate(id);
    }

    @GetMapping("/api/projects/{id}/questions")
    List<QuestionView> list(@PathVariable UUID id) {
        return service.list(id);
    }

    @PutMapping("/api/projects/{id}/answers")
    List<QuestionView> save(@PathVariable UUID id, @RequestBody SaveAnswersRequest request) {
        return service.save(id, request);
    }

    @PostMapping("/api/projects/{id}/question-items")
    @ResponseStatus(HttpStatus.CREATED)
    List<QuestionView> add(@PathVariable UUID id, @RequestBody QuestionDraft.Item item) {
        return service.add(id, item);
    }

    @PutMapping("/api/projects/{id}/question-items/{questionId}")
    List<QuestionView> edit(@PathVariable UUID id, @PathVariable UUID questionId,
            @RequestBody QuestionDraft.Item item) {
        return service.edit(id, questionId, item);
    }

    @DeleteMapping("/api/projects/{id}/question-items/{questionId}")
    List<QuestionView> remove(@PathVariable UUID id, @PathVariable UUID questionId) {
        return service.remove(id, questionId);
    }
}
