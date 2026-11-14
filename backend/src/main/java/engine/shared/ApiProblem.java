package engine.shared;

/** A failure the API answers as problem+json with the given status and a message for people. */
public class ApiProblem extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String title;

    public ApiProblem(int status, String title, String detail, Throwable cause) {
        super(detail, cause);
        this.status = status;
        this.title = title;
    }

    public ApiProblem(int status, String title, String detail) {
        this(status, title, detail, null);
    }

    public int status() {
        return status;
    }

    public String title() {
        return title;
    }
}
