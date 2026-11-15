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

export const builderIds: readonly BuilderId[] = ['claude_code', 'gemini_cli'];

const MODEL_ID = /^[A-Za-z0-9._-]{1,80}$/;

/** A model id the engine accepts for Gemini CLI (the same rule as the API). */
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

/** The value to send for the model typed or picked: trimmed; null when it is not a valid id. */
export function modelToSave(text: string): string | null {
	const trimmed = text.trim();
	return trimmed === '' || isModelId(trimmed) ? trimmed : null;
}
