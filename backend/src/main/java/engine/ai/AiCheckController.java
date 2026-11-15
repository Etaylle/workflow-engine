package engine.ai;

import engine.shared.AiEndpoint;
import engine.shared.AiProvider;
import engine.shared.AiTarget;
import engine.shared.ApiProblem;
import engine.shared.SessionKey;
import jakarta.validation.Valid;
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

    /** Request body; the key never appears in toString. Without a key the saved key is tested. */
    public record AiCheckRequest(String provider, String apiKey, String baseUrl, String model) {
        @Override
        public String toString() {
            return "AiCheckRequest[provider=" + provider + ", apiKey=***]";
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
        AiProvider provider = request.provider() == null ? AiProvider.ANTHROPIC : AiProvider.ofId(request.provider());
        boolean typed = request.apiKey() != null && !request.apiKey().isBlank();
        AiTarget target = typed ? typedTarget(provider, request) : savedTarget(provider);
        String model = connection.check(target);
        if (typed) {
            store(provider, target);
        }
        return new AiCheckResult(true, model);
    }

    private AiTarget typedTarget(AiProvider provider, AiCheckRequest request) {
        String key = request.apiKey().trim();
        if (provider == AiProvider.ANTHROPIC) {
            return new AiTarget(provider, key, null, "claude");
        }
        String url = AiEndpoint.checked(blank(request.baseUrl()) ? sessionKey.defaultBaseUrl() : request.baseUrl());
        String model = blank(request.model()) ? sessionKey.defaultModel() : request.model().strip();
        return new AiTarget(provider, key, url, model);
    }

    private AiTarget savedTarget(AiProvider provider) {
        if (provider == AiProvider.ANTHROPIC) {
            return sessionKey.anthropicKey().map(k -> new AiTarget(provider, k, null, "claude"))
                    .orElseThrow(AiCheckController::noKey);
        }
        return sessionKey.compatible()
                .map(c -> new AiTarget(provider, c.apiKey(), c.baseUrl(), c.model()))
                .orElseThrow(AiCheckController::noKey);
    }

    private void store(AiProvider provider, AiTarget target) {
        if (provider == AiProvider.ANTHROPIC) {
            sessionKey.setAnthropic(target.apiKey());
        } else {
            sessionKey.setCompatible(target.apiKey(), target.baseUrl(), target.model());
        }
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    private static ApiProblem noKey() {
        return new ApiProblem(400, "No key to test", "Type a key to test, or save one first.");
    }

    @ExceptionHandler(AiCheckException.class)
    ResponseEntity<ProblemDetail> failed(AiCheckException e) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(e.status()), e.getMessage());
        problem.setTitle("AI check failed");
        return ResponseEntity.status(e.status()).body(problem);
    }
}
