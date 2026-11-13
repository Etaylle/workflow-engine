package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum TestResult {
    WAITING("waiting"),
    RUNNING("running"),
    PASSED("passed"),
    FAILED("failed");

    private final String dbValue;

    TestResult(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static TestResult fromDb(String value) {
        for (TestResult v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown TestResult: " + value);
    }
}
