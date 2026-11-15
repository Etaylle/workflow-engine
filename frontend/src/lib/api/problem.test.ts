import { describe, expect, it } from 'vitest';
import { ApiError } from './client.js';
import { describeError } from './problem.js';

const failure = (status: number, title: string) => new ApiError({ status, title, detail: 'x' });

describe('describeError', () => {
	it('maps the known problems to messages', () => {
		expect(describeError(failure(409, 'AI key missing')).key).toBe('error.keyMissing');
		expect(describeError(failure(401, 'Anthropic key rejected')).key).toBe('error.keyRejected');
		expect(describeError(failure(429, 'Rate limit reached'))).toEqual({
			key: 'error.rateLimit',
			params: { detail: 'x' }
		});
		expect(describeError(failure(502, 'Invalid questions from the AI')).key).toBe(
			'error.invalidOutput'
		);
	});

	it('shows the server text for other problems and a network failure as unreachable', () => {
		expect(describeError(failure(409, 'Nothing to confirm'))).toEqual({
			key: 'error.other',
			params: { detail: 'x' }
		});
		expect(describeError(new TypeError('network')).key).toBe('error.unreachable');
	});
});
