package engine.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Operation checkAiConnection of api/openapi.yaml. */
@RestController
public class AiCheckController {

    /** Request body; the key never appears in toString. */
    public record AiCheckRequest(@NotBlank String apiKey) {
        @Override
        public String toString() {
            return "AiCheckRequest[apiKey=***]";
        }
    }

    public record AiCheckResult(boolean ok, String model) {
    }

    private final AiConnection connection;
    private final SessionKey sessionKey;

    public AiCheckController(AiConnection connection, SessionKey sessionKey) {
        this.connection = connection;
        this.sessionKey = sessionKey;
    }

    @PostMapping("/api/ai/check")
    public AiCheckResult check(@Valid @RequestBody AiCheckRequest request) {
        String key = request.apiKey().trim();
        try {
            String model = connection.check(key);
            sessionKey.set(key);
            return new AiCheckResult(true, model);
        } catch (AiCheckException e) {
            sessionKey.clear();
            throw e;
        }
    }

    @ExceptionHandler(AiCheckException.class)
    ResponseEntity<ProblemDetail> failed(AiCheckException e) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(e.status()), e.getMessage());
        problem.setTitle("AI check failed");
        return ResponseEntity.status(e.status()).body(problem);
    }
}
