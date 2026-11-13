package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum ReportKind {
    TASK_SPEC("task_spec"),
    REPORT("report"),
    BLOCKER("blocker");

    private final String dbValue;

    ReportKind(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static ReportKind fromDb(String value) {
        for (ReportKind v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown ReportKind: " + value);
    }
}
