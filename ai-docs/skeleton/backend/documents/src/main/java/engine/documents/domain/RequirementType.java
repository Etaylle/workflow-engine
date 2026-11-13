package engine.documents.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum RequirementType {
    FUNCTIONAL("functional"),
    QUALITY("quality"),
    CONSTRAINT("constraint");

    private final String dbValue;

    RequirementType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static RequirementType fromDb(String value) {
        for (RequirementType v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown RequirementType: " + value);
    }
}
