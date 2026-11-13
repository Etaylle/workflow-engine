package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum HumanQuestionStatus {
    OPEN("open"),
    ANSWERED("answered"),
    CANCELLED("cancelled");

    private final String dbValue;

    HumanQuestionStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static HumanQuestionStatus fromDb(String value) {
        for (HumanQuestionStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown HumanQuestionStatus: " + value);
    }
}
