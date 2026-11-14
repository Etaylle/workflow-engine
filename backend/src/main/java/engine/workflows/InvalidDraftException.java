package engine.workflows;

/** The workflow Claude wrote is not usable; the message says why and is sent back on the retry. */
public class InvalidDraftException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidDraftException(String message) {
        super(message);
    }

    public InvalidDraftException(String message, Throwable cause) {
        super(message, cause);
    }
}
