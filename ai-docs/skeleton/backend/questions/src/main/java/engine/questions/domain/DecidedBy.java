package engine.questions.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum DecidedBy {
    CLIENT("client"),
    AI("ai");

    private final String dbValue;

    DecidedBy(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static DecidedBy fromDb(String value) {
        for (DecidedBy v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown DecidedBy: " + value);
    }
}
