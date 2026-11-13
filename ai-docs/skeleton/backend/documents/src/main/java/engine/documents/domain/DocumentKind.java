package engine.documents.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum DocumentKind {
    REQUIREMENTS("requirements"),
    PROJECT_PLAN("project_plan");

    private final String dbValue;

    DocumentKind(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static DocumentKind fromDb(String value) {
        for (DocumentKind v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentKind: " + value);
    }
}
