package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum LensVerdict {
    PASS("pass"),
    PASS_WITH_FINDINGS("pass_with_findings"),
    FAIL("fail"),
    NOT_YET("not_yet");

    private final String dbValue;

    LensVerdict(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static LensVerdict fromDb(String value) {
        for (LensVerdict v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown LensVerdict: " + value);
    }
}
