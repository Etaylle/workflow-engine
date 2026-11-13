package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum Severity {
    CRITICAL("critical"),
    MAJOR("major"),
    MINOR("minor"),
    OBSERVATION("observation");

    private final String dbValue;

    Severity(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static Severity fromDb(String value) {
        for (Severity v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown Severity: " + value);
    }
}
