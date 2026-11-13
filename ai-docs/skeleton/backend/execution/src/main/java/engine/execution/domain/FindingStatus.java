package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum FindingStatus {
    OPEN("open"),
    FIXED("fixed"),
    ACCEPTED("accepted");

    private final String dbValue;

    FindingStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static FindingStatus fromDb(String value) {
        for (FindingStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown FindingStatus: " + value);
    }
}
