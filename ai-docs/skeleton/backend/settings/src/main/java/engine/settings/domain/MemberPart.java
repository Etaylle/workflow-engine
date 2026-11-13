package engine.settings.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum MemberPart {
    DOES("does"),
    CHECKS("checks");

    private final String dbValue;

    MemberPart(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static MemberPart fromDb(String value) {
        for (MemberPart v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown MemberPart: " + value);
    }
}
