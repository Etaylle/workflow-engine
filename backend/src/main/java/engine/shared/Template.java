package engine.shared;

import java.nio.file.Path;

/**
 * A project template the engine can copy into a workspace (declared by its template.json).
 *
 * @param id folder name and identifier, for example sveltekit-sqlite
 * @param name name for people
 * @param kind console or web
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

    /** True for a console program: no port, no web page. */
    public boolean isConsole() {
        return CONSOLE.equals(kind);
    }
}
