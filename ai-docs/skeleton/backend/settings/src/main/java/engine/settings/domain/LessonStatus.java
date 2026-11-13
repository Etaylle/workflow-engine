package engine.settings.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum LessonStatus {
    PROPOSED("proposed"),
    APPLIED("applied"),
    DISMISSED("dismissed");

    private final String dbValue;

    LessonStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static LessonStatus fromDb(String value) {
        for (LessonStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown LessonStatus: " + value);
    }
}
