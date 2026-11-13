package engine.settings.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum TeamPreset {
    STARTER("starter"),
    STANDARD("standard"),
    FULL("full"),
    CUSTOM("custom");

    private final String dbValue;

    TeamPreset(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static TeamPreset fromDb(String value) {
        for (TeamPreset v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown TeamPreset: " + value);
    }
}
