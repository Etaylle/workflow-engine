import { describe, expect, it } from 'vitest';
import { statusKey, statusTone } from './status.js';

describe('status', () => {
	it('maps a known status to its message and an unknown one to "other"', () => {
		expect(statusKey('answered')).toBe('status.answered');
		expect(statusKey('executing')).toBe('status.executing');
		expect(statusKey('stopped')).toBe('status.other');
	});
	it('colours waiting, finished and running', () => {
		expect(statusTone('questions_ready')).toBe('wait');
		expect(statusTone('confirmed')).toBe('done');
		expect(statusTone('draft')).toBe('');
		expect(statusTone('workflow_drafted')).toBe('active');
	});
});
