package engine.execution.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum RunStatus {
    QUEUED("queued"),
    PREPARING("preparing"),
    EXECUTING("executing"),
    PAUSED("paused"),
    WAITING_FOR_HUMAN("waiting_for_human"),
    RESUMING("resuming"),
    DONE("done"),
    STOPPED("stopped"),
    FAILED("failed");

    private final String dbValue;

    RunStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static RunStatus fromDb(String value) {
        for (RunStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown RunStatus: " + value);
    }
}
