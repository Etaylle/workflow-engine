package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum CiRunStatus {
    RUNNING("running"),
    PASSED("passed"),
    FAILED("failed");

    private final String dbValue;

    CiRunStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static CiRunStatus fromDb(String value) {
        for (CiRunStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown CiRunStatus: " + value);
    }
}
