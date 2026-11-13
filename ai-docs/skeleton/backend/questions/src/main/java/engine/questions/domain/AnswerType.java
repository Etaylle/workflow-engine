package engine.questions.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum AnswerType {
    SINGLE_CHOICE("single_choice"),
    MULTIPLE_CHOICE("multiple_choice"),
    TEXT("text");

    private final String dbValue;

    AnswerType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static AnswerType fromDb(String value) {
        for (AnswerType v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown AnswerType: " + value);
    }
}
