package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum TestLevel {
    UNIT("unit"),
    COMPONENT("component"),
    INTEGRATION("integration"),
    CONTRACT("contract"),
    FRONTEND_INTEGRATION("frontend_integration"),
    END_TO_END("end_to_end"),
    SMOKE("smoke");

    private final String dbValue;

    TestLevel(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static TestLevel fromDb(String value) {
        for (TestLevel v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown TestLevel: " + value);
    }
}
