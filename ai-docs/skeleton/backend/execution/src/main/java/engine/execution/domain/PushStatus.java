package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum PushStatus {
    NOT_PUSHED("not_pushed"),
    PUSHED("pushed"),
    FAILED("failed");

    private final String dbValue;

    PushStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static PushStatus fromDb(String value) {
        for (PushStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown PushStatus: " + value);
    }
}
