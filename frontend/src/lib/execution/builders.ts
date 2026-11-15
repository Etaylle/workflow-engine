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
