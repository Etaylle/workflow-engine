package engine.shared;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns an {@link ApiProblem} into application/problem+json. */
@RestControllerAdvice
class ApiProblemHandler {

    @ExceptionHandler(ApiProblem.class)
    ResponseEntity<ProblemDetail> handle(ApiProblem e) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(e.status()), e.getMessage());
        problem.setTitle(e.title());
        return ResponseEntity.status(e.status()).body(problem);
    }
}
