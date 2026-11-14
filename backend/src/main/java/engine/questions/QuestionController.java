package engine.questions;

import engine.questions.QuestionService.SaveAnswersRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
