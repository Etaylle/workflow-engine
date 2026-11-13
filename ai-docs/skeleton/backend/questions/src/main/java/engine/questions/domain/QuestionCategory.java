package engine.questions.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum QuestionCategory {
    REQUIREMENT("requirement"),
    FEATURE("feature");

    private final String dbValue;

    QuestionCategory(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static QuestionCategory fromDb(String value) {
        for (QuestionCategory v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown QuestionCategory: " + value);
    }
}
