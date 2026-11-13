package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum ConsoleLevel {
    INFO("info"),
    WARN("warn"),
    ERROR("error"),
    SUCCESS("success");

    private final String dbValue;

    ConsoleLevel(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static ConsoleLevel fromDb(String value) {
        for (ConsoleLevel v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown ConsoleLevel: " + value);
    }
}
