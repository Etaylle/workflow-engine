package engine.shared;

/** What Claude wrote is not usable; the message says why and is sent back on the retry. */
public class InvalidAiOutputException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidAiOutputException(String message) {
        super(message);
    }

    public InvalidAiOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
