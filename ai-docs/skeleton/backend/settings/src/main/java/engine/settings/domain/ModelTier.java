package engine.settings.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum ModelTier {
    BEST("best"),
    STANDARD("standard");

    private final String dbValue;

    ModelTier(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static ModelTier fromDb(String value) {
        for (ModelTier v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown ModelTier: " + value);
    }
}
