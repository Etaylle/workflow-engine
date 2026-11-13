package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum StepRunStatus {
    WAITING("waiting"),
    RUNNING("running"),
    DONE("done"),
    NOT_NEEDED("not_needed"),
    FAILED("failed"),
    SKIPPED("skipped");

    private final String dbValue;

    StepRunStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static StepRunStatus fromDb(String value) {
        for (StepRunStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown StepRunStatus: " + value);
    }
}
