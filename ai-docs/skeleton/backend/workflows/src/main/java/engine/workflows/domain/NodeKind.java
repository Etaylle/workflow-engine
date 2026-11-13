package engine.workflows.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum NodeKind {
    START_EVENT("start_event"),
    TASK("task"),
    END_EVENT("end_event");

    private final String dbValue;

    NodeKind(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static NodeKind fromDb(String value) {
        for (NodeKind v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown NodeKind: " + value);
    }
}
