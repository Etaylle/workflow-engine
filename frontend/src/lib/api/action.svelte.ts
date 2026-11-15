import { locale } from '#lib/i18n/state.svelte.js';
import { isKeyProblem } from '#lib/ai/keyState.js';
import { describeError } from './problem.js';

/** A button action with a busy flag and a translated error message. */
export function createAction() {
	let busy = $state(false);
	let error = $state<string | null>(null);
	let keyIssue = $state(false);
	return {
		get busy() {
			return busy;
		},
		get error() {
			return error;
		},
		/** True when the error is a missing or rejected key: a new key fixes it. */
		get keyIssue() {
			return keyIssue;
		},
		clear() {
			error = null;
			keyIssue = false;
		},
		fail(message: string) {
			error = message;
			keyIssue = false;
		},
		/** Runs the call; returns true when it succeeded. */
		async run(call: () => Promise<unknown>): Promise<boolean> {
			busy = true;
			error = null;
			keyIssue = false;
			try {
				await call();
				return true;
			} catch (e) {
				const { key, params } = describeError(e);
				error = locale.t(key, params);
				keyIssue = isKeyProblem(key);
				return false;
			} finally {
				busy = false;
			}
		}
	};
}
