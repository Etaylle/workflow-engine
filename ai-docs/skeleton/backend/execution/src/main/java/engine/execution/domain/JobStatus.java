package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum JobStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    DONE("done"),
    FAILED("failed");

    private final String dbValue;

    JobStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static JobStatus fromDb(String value) {
        for (JobStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown JobStatus: " + value);
    }
}
