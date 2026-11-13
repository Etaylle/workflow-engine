package engine.projects.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum ProjectStatus {
    DRAFT("draft"),
    QUESTIONS_READY("questions_ready"),
    ANSWERED("answered"),
    REQUIREMENTS_READY("requirements_ready"),
    WORKFLOW_DRAFTED("workflow_drafted"),
    CONFIRMED("confirmed"),
    EXECUTING("executing"),
    DONE("done"),
    STOPPED("stopped"),
    FAILED("failed");

    private final String dbValue;

    ProjectStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static ProjectStatus fromDb(String value) {
        for (ProjectStatus v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown ProjectStatus: " + value);
    }
}
