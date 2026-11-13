package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum TaskRunStatus {
    PENDING("pending"),
    RUNNING("running"),
    FIXING("fixing"),
    WAITING_FOR_HUMAN("waiting_for_human"),
    DONE("done"),
    FAILED("failed"),
    SKIPPED("skipped"),
    MANUAL("manual");

    private final String dbValue;

    TaskRunStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static TaskRunStatus fromDb(String value) {
        for (TaskRunStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown TaskRunStatus: " + value);
    }
}
