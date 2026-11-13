package engine.documents.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum Priority {
    MUST("must"),
    SHOULD("should"),
    COULD("could"),
    WONT("wont");

    private final String dbValue;

    Priority(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static Priority fromDb(String value) {
        for (Priority v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown Priority: " + value);
    }
}
