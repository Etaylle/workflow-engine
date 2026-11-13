package engine.workflows.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum TaskType {
    SOFTWARE("software"),
    MANUAL("manual");

    private final String dbValue;

    TaskType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static TaskType fromDb(String value) {
        for (TaskType v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown TaskType: " + value);
    }
}
