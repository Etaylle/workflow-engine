import type { MessageKey } from '#lib/i18n/i18n.js';
import type { AiKeyState } from '#lib/api/client.js';

/** The message that tells the header what the engine holds; never more than the last 4 characters. */
export function describeKey(state: AiKeyState | null): {
	key: MessageKey;
	params: Record<string, string>;
} {
	if (state === null) return { key: 'key.unknown', params: {} };
	if (!state.set) return { key: 'key.none', params: {} };
	return state.last4
		? { key: 'key.setEnding', params: { last4: state.last4 } }
		: { key: 'key.set', params: {} };
}

/** Failures that a new key fixes: the page then offers to open the key panel. */
export function isKeyProblem(key: MessageKey): boolean {
	return key === 'error.keyMissing' || key === 'error.keyRejected';
}

/** The typed key without surrounding blanks, or null when there is nothing to save or test. */
export function typedKey(input: string): string | null {
	const key = input.trim();
	return key === '' ? null : key;
}
