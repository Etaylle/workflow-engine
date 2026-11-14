package engine.ai;

/** The AI check failed; carries the HTTP status the API answers with. */
public class AiCheckException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public AiCheckException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
