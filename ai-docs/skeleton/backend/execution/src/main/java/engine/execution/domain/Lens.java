package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum Lens {
    TECHNICAL("technical"),
    TESTS("tests"),
    BUSINESS("business");

    private final String dbValue;

    Lens(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static Lens fromDb(String value) {
        for (Lens v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown Lens: " + value);
    }
}
