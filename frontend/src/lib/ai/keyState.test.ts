import { describe, expect, it } from 'vitest';
import { describeKey, isKeyProblem, typedKey } from './keyState.js';

describe('describeKey', () => {
	it('says no key when none is set', () => {
		expect(describeKey({ set: false, last4: null }).key).toBe('key.none');
	});
	it('shows only the last four characters', () => {
		expect(describeKey({ set: true, last4: '1234' })).toEqual({
			key: 'key.setEnding',
			params: { last4: '1234' }
		});
	});
	it('shows no suffix for a short key', () => {
		expect(describeKey({ set: true, last4: '' }).key).toBe('key.set');
	});
	it('is unknown before the engine answered', () => {
		expect(describeKey(null).key).toBe('key.unknown');
	});
});

describe('isKeyProblem', () => {
	it('is true for a missing or rejected key only', () => {
		expect(isKeyProblem('error.keyMissing')).toBe(true);
		expect(isKeyProblem('error.keyRejected')).toBe(true);
		expect(isKeyProblem('error.rateLimit')).toBe(false);
	});
});

describe('typedKey', () => {
	it('trims and refuses blanks', () => {
		expect(typedKey('  sk-x ')).toBe('sk-x');
		expect(typedKey('   ')).toBeNull();
	});
});
