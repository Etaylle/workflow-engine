import type { MessageKey } from '#lib/i18n/i18n.js';
import { ApiError } from './client.js';

export type Described = { key: MessageKey; params: Record<string, string> };

/** Maps a failed call to a message of the message files; unknown failures show the server text. */
export function describeError(error: unknown): Described {
	if (!(error instanceof ApiError)) return { key: 'error.unreachable', params: {} };
	const { status, title } = error.problem;
	if (status === 409 && title === 'Anthropic key missing')
		return { key: 'error.keyMissing', params: {} };
	if (status === 401) return { key: 'error.keyRejected', params: {} };
	if (status === 429) return { key: 'error.rateLimit', params: {} };
	if (status === 502 && title === 'Invalid workflow from Claude')
		return { key: 'error.invalidOutput', params: {} };
	return { key: 'error.other', params: { detail: error.message } };
}
