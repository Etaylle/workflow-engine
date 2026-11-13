package engine.projects.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum AttachmentKind {
    FILE("file"),
    LINK("link");

    private final String dbValue;

    AttachmentKind(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static AttachmentKind fromDb(String value) {
        for (AttachmentKind v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown AttachmentKind: " + value);
    }
}
