package engine.projects.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum AgentHarness {
    OPENCODE("opencode"),
    CLAUDE_CODE("claude_code");

    private final String dbValue;

    AgentHarness(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static AgentHarness fromDb(String value) {
        for (AgentHarness v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown AgentHarness: " + value);
    }
}
