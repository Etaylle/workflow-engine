package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum AgentCallStatus {
    OK("ok"),
    INVALID_OUTPUT("invalid_output"),
    ERROR("error");

    private final String dbValue;

    AgentCallStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static AgentCallStatus fromDb(String value) {
        for (AgentCallStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown AgentCallStatus: " + value);
    }
}
