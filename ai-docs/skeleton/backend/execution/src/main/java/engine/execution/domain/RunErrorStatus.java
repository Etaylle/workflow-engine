package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum RunErrorStatus {
    OPEN("open"),
    FIXED("fixed"),
    NEEDS_HUMAN("needs_human"),
    SKIPPED("skipped");

    private final String dbValue;

    RunErrorStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static RunErrorStatus fromDb(String value) {
        for (RunErrorStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown RunErrorStatus: " + value);
    }
}
