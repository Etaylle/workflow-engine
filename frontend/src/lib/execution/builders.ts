import type {
	BuilderId,
	BuilderReadiness,
	ClaudeAuthMode,
	Prerequisites
} from '#lib/api/execution.js';

/** What the client can do about one missing thing. */
export type MissingAction =
	{ kind: 'open_key_panel' } | { kind: 'use_machine_sign_in' } | { kind: 'none' };

/** The action that fixes a missing code; unknown codes get none. */
export function actionFor(code: string): MissingAction {
	switch (code) {
		case 'anthropic_key':
		case 'gemini_key':
			return { kind: 'open_key_panel' };
		case 'machine_sign_in':
			return { kind: 'use_machine_sign_in' };
		default:
			return { kind: 'none' };
	}
}

export function readinessOf(
	prerequisites: Prerequisites | null,
	builder: string
): BuilderReadiness | null {
	return prerequisites?.builders.find((b) => b.id === builder) ?? null;
}

/**
 * Why the Run button must wait, or null when it may start. Without prerequisites (an engine that
 * does not report them) nothing blocks: the engine itself answers on start.
 */
export function startBlock(
	prerequisites: Prerequisites | null,
	builder: string
): { builder: BuilderId; missing: string[] } | null {
	const readiness = readinessOf(prerequisites, builder);
	if (!readiness || readiness.ready) return null;
	return { builder: readiness.id, missing: readiness.missing };
}

/** The auth choices of Claude Code with whether each one is covered by what is missing. */
export function claudeAuthOptions(
	prerequisites: Prerequisites
): { mode: ClaudeAuthMode; ready: boolean; selected: boolean }[] {
	const claude = readinessOf(prerequisites, 'claude_code');
	const missing = claude?.missing ?? [];
	return (['anthropic_key', 'machine_sign_in'] as const).map((mode) => ({
		mode,
		ready: !missing.includes(mode),
		selected: prerequisites.claudeAuth === mode
	}));
}
