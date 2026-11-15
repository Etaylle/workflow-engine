import type { Builder, BuilderId, Builders, ClaudeAuthMode } from '#lib/api/execution.js';

/** What the client can do about one missing thing. */
export type MissingAction = 'open_key_panel' | 'none';

/** The action that fixes a missing code; unknown codes and the sign-in (done in a terminal) get none. */
export function actionFor(code: string): MissingAction {
	return code === 'no_anthropic_key' || code === 'no_gemini_key' ? 'open_key_panel' : 'none';
}

export function builderOf(builders: Builders | null, id: string): Builder | null {
	return builders?.builders.find((b) => b.id === id) ?? null;
}

/**
 * The builder that may not start yet, or null when it may. Without a report (the engine does not
 * answer) nothing blocks: the start itself answers 409 with the same information.
 */
export function startBlock(builders: Builders | null, id: string): Builder | null {
	const builder = builderOf(builders, id);
	return builder && !builder.ready ? builder : null;
}

/** The other way Claude Code can sign in, offered next to the current one. */
export function otherAuthMode(builder: Builder): ClaudeAuthMode {
	return builder.authMode === 'machine_sign_in' ? 'anthropic_key' : 'machine_sign_in';
}

/** Short reason shown beside the disabled Run button: the missing codes of the chosen builder. */
export function missingCodes(builder: Builder): string[] {
	return builder.missing.map((item) => item.code);
}

export const builderIds: readonly BuilderId[] = ['claude_code', 'gemini_cli', 'codex_cli'];

const MODEL_ID = /^[A-Za-z0-9._-]{1,80}$/;

/** A model id the engine accepts for Gemini CLI and Codex CLI (the same rule as the API). */
export const isModelId = (text: string): boolean => MODEL_ID.test(text);

/**
 * The model and effort of a builder choice for a short line, "fable · high": the chosen model or the
 * default one, then the effort when one was chosen. Empty parts are left out.
 */
export function modelEffortText(
	model: string | null | undefined,
	effort: string | null | undefined
): string {
	return [model, effort].filter(Boolean).join(' · ');
}

/**
 * What a change of the builder does to the choice made so far: a new builder starts with the
 * defaults (the engine clears both on the project in the same way).
 */
export function choiceAfterBuilderChange(
	oldId: string,
	newId: string,
	choice: { model: string; effort: string }
): { model: string; effort: string } {
	return oldId === newId ? choice : { model: '', effort: '' };
}

/** The second builder of a project: an empty harness means none (the builder checks its own work). */
export type Reviewer = { harness: string; model: string; effort: string };

export const noReviewer: Reviewer = { harness: '', model: '', effort: '' };

/** The reviewer a project has chosen, with empty strings for the defaults. */
export function reviewerOf(project: {
	reviewerHarness: string | null;
	reviewerModel: string | null;
	reviewerEffort: string | null;
}): Reviewer {
	return {
		harness: project.reviewerHarness ?? '',
		model: project.reviewerModel ?? '',
		effort: project.reviewerEffort ?? ''
	};
}

/** The builder offered first when the switch goes on: the first one that is not the builder. */
export function defaultReviewerHarness(builderHarness: string): string {
	return builderIds.find((id) => id !== builderHarness) ?? builderIds[0]!;
}

/**
 * True when the reviewer would be the same builder with the same model as the one that writes the
 * software (the engine answers 400 for that). Two empty models are both the default of the builder.
 */
export function reviewerConflict(
	builder: { harness: string; model: string },
	reviewer: Reviewer
): boolean {
	return (
		reviewer.harness !== '' &&
		reviewer.harness === builder.harness &&
		reviewer.model === builder.model
	);
}

/** The reviewer after a change of its harness: a new harness starts with the defaults. */
export function reviewerAfterHarnessChange(old: Reviewer, harness: string): Reviewer {
	return old.harness === harness ? old : { harness, model: '', effort: '' };
}

/** The value to send for the model typed or picked: trimmed; null when it is not a valid id. */
export function modelToSave(text: string): string | null {
	const trimmed = text.trim();
	return trimmed === '' || isModelId(trimmed) ? trimmed : null;
}
