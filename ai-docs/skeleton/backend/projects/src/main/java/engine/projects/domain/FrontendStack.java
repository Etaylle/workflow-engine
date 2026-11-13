package engine.projects.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum FrontendStack {
    SVELTE("svelte"),
    ANGULAR("angular"),
    NUXT("nuxt");

    private final String dbValue;

    FrontendStack(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static FrontendStack fromDb(String value) {
        for (FrontendStack v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown FrontendStack: " + value);
    }
}
