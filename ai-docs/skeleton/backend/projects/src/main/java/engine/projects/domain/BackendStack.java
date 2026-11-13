package engine.projects.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum BackendStack {
    JAVA("java"),
    CSHARP("csharp"),
    PYTHON("python");

    private final String dbValue;

    BackendStack(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static BackendStack fromDb(String value) {
        for (BackendStack v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown BackendStack: " + value);
    }
}
