import { getAiKeys, getStatus, type AiKeys, type AiProviderId } from '#lib/api/client.js';
import { providerName } from './keyState.js';

/** What the engine holds for the session and whether it runs in demo mode; shared by all screens. */
export const engineKey = (() => {
	let keys = $state<AiKeys | null>(null);
	let panelOpen = $state(false);
	let demo = $state(false);
	return {
		get keys() {
			return keys;
		},
		get panelOpen() {
			return panelOpen;
		},
		get demo() {
			return demo;
		},
		/** Whether the engine holds any key. */
		get anySet() {
			return keys !== null && (keys.anthropic.set || keys.openai.set);
		},
		/** Display name of a provider; the first argument is the word for Anthropic's Claude. */
		nameOf(claude: string, provider: AiProviderId): string {
			return provider === 'anthropic' || keys === null
				? claude
				: (providerName(keys, provider) ?? claude);
		},
		openPanel() {
			panelOpen = true;
		},
		closePanel() {
			panelOpen = false;
		},
		adopt(next: AiKeys) {
			keys = next;
		},
		async refresh() {
			try {
				keys = await getAiKeys();
			} catch {
				keys = null;
			}
		},
		async loadMode() {
			try {
				demo = (await getStatus()).aiStub;
			} catch {
				demo = false;
			}
		}
	};
})();
