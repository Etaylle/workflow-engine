package engine.settings.domain;

/** Values of the database column CHECK; dbValue() is the stored text. */
public enum RunsWhen {
    ALWAYS("always"),
    IF_SCREEN("if_screen"),
    IF_INTERFACE_CHANGES("if_interface_changes"),
    IF_DATA_CHANGES("if_data_changes"),
    IF_NEEDED("if_needed"),
    AFTER_EVERY_CHANGE("after_every_change"),
    ON_FAILURE("on_failure");

    private final String dbValue;

    RunsWhen(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static RunsWhen fromDb(String value) {
        for (RunsWhen v : values()) {
            if (v.dbValue.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown RunsWhen: " + value);
    }
}
