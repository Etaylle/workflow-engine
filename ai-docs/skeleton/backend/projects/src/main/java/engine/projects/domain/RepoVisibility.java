package engine.projects.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum RepoVisibility {
    PUBLIC("public");

    private final String dbValue;

    RepoVisibility(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static RepoVisibility fromDb(String value) {
        for (RepoVisibility v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown RepoVisibility: " + value);
    }
}
