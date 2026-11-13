package engine.documents.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum DocumentFormat {
    DOCX("docx"),
    MD("md"),
    PDF("pdf");

    private final String dbValue;

    DocumentFormat(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static DocumentFormat fromDb(String value) {
        for (DocumentFormat v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentFormat: " + value);
    }
}
