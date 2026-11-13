package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum ErrorClass {
    TEMPORARY("temporary"),
    BROKEN_ENVIRONMENT("broken_environment"),
    FLAKY_TEST("flaky_test"),
    REAL_DEFECT("real_defect"),
    REQUIREMENT_CONFLICT("requirement_conflict");

    private final String dbValue;

    ErrorClass(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static ErrorClass fromDb(String value) {
        for (ErrorClass v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorClass: " + value);
    }
}
