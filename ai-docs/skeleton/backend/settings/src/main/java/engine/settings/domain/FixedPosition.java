package engine.settings.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum FixedPosition {
    FIRST("first"),
    LAST("last"),
    NONE("none");

    private final String dbValue;

    FixedPosition(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static FixedPosition fromDb(String value) {
        for (FixedPosition v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown FixedPosition: " + value);
    }
}
