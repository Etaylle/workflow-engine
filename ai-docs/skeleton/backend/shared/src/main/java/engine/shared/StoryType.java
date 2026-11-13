package engine.shared;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum StoryType {
    FEATURE("feature"),
    TECHNICAL("technical"),
    DEFECT("defect"),
    SPIKE("spike"),
    AUDIT("audit");

    private final String dbValue;

    StoryType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static StoryType fromDb(String value) {
        for (StoryType v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown StoryType: " + value);
    }
}
