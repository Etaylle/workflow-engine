package engine.shared;

import java.nio.file.Path;

/**
 * A project template the engine can copy into a workspace (declared by its template.json).
 *
 * @param id folder name and identifier, for example sveltekit-sqlite
 * @param name name for people
 * @param kind console, web, or any when the builder chooses and declares it in app.json
 * @param description one sentence
 * @param needsDatabase true when the program needs a PostgreSQL database made by the engine
 * @param dir the folder of the template
 */
public record Template(String id, String name, String kind, String description, boolean needsDatabase,
        Path dir) {

    /** Kind of a program that prints to the console and ends. */
    public static final String CONSOLE = "console";
    /** Kind of a program that serves a web page and answers requests. */
    public static final String WEB = "web";

    /** Kind of a template that leaves the choice to the builder, who declares it in app.json. */
    public static final String ANY = "any";

    private static final java.util.regex.Pattern DECLARED =
            java.util.regex.Pattern.compile("\"kind\"\\s*:\\s*\"(console|web)\"");

    /** True for a console program: no port, no web page. */
    public boolean isConsole() {
        return CONSOLE.equals(kind);
    }

    /** True when the builder chooses the technology and the kind of software. */
    public boolean isFree() {
        return ANY.equals(kind);
    }

    /**
     * The kind of the software in a project folder: the kind of the template, or for a free
     * template what app.json of the folder declares (web when it is missing or unreadable).
     */
    public String kindIn(Path project) {
        if (!isFree()) {
            return kind;
        }
        try {
            java.util.regex.Matcher declared = DECLARED.matcher(
                    java.nio.file.Files.readString(project.resolve("app.json")));
            return declared.find() ? declared.group(1) : WEB;
        } catch (java.io.IOException e) {
            return WEB;
        }
    }
}
