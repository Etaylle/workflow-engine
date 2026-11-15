package engine.execution;

import engine.shared.Harness;

/**
 * Which builder, model and effort ran something: the reviewer of a run, or one agent call.
 *
 * @param harness the builder
 * @param model the model really used (the default resolved), or null
 * @param effort the effort level, or null for the CLI's own default
 */
record BuilderRun(Harness harness, String model, String effort) {
}
