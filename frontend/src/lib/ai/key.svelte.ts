import { getAiKey, getStatus, type AiKeyState } from '#lib/api/client.js';

/** What the engine holds for the session and whether it runs in demo mode; shared by all screens. */
export const engineKey = (() => {
	let state = $state<AiKeyState | null>(null);
	let panelOpen = $state(false);
	let demo = $state(false);
	return {
		get state() {
			return state;
		},
		get panelOpen() {
			return panelOpen;
		},
		get demo() {
			return demo;
		},
		openPanel() {
			panelOpen = true;
		},
		closePanel() {
			panelOpen = false;
		},
		async refresh() {
			try {
				state = await getAiKey();
			} catch {
				state = null;
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
