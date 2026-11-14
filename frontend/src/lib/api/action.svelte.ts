import { locale } from '#lib/i18n/state.svelte.js';
import { describeError } from './problem.js';

/** A button action with a busy flag and a translated error message. */
export function createAction() {
	let busy = $state(false);
	let error = $state<string | null>(null);
	return {
		get busy() {
			return busy;
		},
		get error() {
			return error;
		},
		clear() {
			error = null;
		},
		fail(message: string) {
			error = message;
		},
		/** Runs the call; returns true when it succeeded. */
		async run(call: () => Promise<unknown>): Promise<boolean> {
			busy = true;
			error = null;
			try {
				await call();
				return true;
			} catch (e) {
				const { key, params } = describeError(e);
				error = locale.t(key, params);
				return false;
			} finally {
				busy = false;
			}
		}
	};
}
