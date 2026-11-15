import type { MessageKey } from '#lib/i18n/i18n.js';
import type { AiKeys, AiProviderId } from '#lib/api/client.js';
import { presetOfUrl } from './presets.js';

type Described = { key: MessageKey; params: Record<string, string> };

/** Name of the active planning provider for the header and the planning screens. */
export function providerName(keys: AiKeys, provider: AiProviderId): string | null {
	if (provider === 'anthropic') return null;
	return presetOfUrl(keys.openai.baseUrl).name || new URL(keys.openai.baseUrl ?? 'https://x').host;
}

/** What the header button says: the active AI and the end of its key, or that there is no key. */
export function describeKey(keys: AiKeys | null, nameOf: (p: AiProviderId) => string): Described {
	if (keys?.planning == null) return { key: 'key.noKey', params: {} };
	const provider = keys.planning.provider;
	const state = provider === 'anthropic' ? keys.anthropic : keys.openai;
	const name = nameOf(provider);
	return state.last4
		? { key: 'key.keyEnding', params: { name, last4: state.last4 } }
		: { key: 'key.keyShort', params: { name } };
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

/** A friendly warning when a key was typed into the wrong field; null when it looks right. */
export function keyWarning(slot: AiProviderId, key: string): MessageKey | null {
	const typed = key.trim();
	if (typed === '') return null;
	const anthropicLike = typed.startsWith('sk-ant-');
	if (slot === 'openai' && anthropicLike) return 'key.warnWrongSlotOpenai';
	if (slot === 'anthropic' && !anthropicLike) return 'key.warnWrongSlotAnthropic';
	return null;
}
