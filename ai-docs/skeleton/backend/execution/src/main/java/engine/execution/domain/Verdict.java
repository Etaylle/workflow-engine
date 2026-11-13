package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum Verdict {
    PASS("pass"),
    FAIL("fail"),
    NOT_APPLICABLE("not_applicable");

    private final String dbValue;

    Verdict(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static Verdict fromDb(String value) {
        for (Verdict v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown Verdict: " + value);
    }
}
