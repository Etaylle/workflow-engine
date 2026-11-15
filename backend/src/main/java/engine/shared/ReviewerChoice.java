package engine.shared;

/**
 * The second builder of a project, which runs the independent review steps (D-103).
 *
 * @param harness the reviewer's builder
 * @param choice its model and effort; null values mean the defaults
 */
public record ReviewerChoice(Harness harness, BuilderChoice choice) {
}
