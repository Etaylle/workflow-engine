package engine.workflows.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum WorkflowStatus {
    DRAFT("draft"),
    VALID("valid"),
    INVALID("invalid"),
    CONFIRMED("confirmed");

    private final String dbValue;

    WorkflowStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static WorkflowStatus fromDb(String value) {
        for (WorkflowStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown WorkflowStatus: " + value);
    }
}
