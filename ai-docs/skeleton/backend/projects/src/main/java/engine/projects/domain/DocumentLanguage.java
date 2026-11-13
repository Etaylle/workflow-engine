package engine.projects.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum DocumentLanguage {
    EN("en"),
    DE("de");

    private final String dbValue;

    DocumentLanguage(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static DocumentLanguage fromDb(String value) {
        for (DocumentLanguage v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentLanguage: " + value);
    }
}
