package engine.shared;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum Provider {
    ANTHROPIC("anthropic");

    private final String dbValue;

    Provider(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static Provider fromDb(String value) {
        for (Provider v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown Provider: " + value);
    }
}
