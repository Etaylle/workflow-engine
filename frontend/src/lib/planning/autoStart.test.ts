import { describe, expect, it } from 'vitest';
import {
	claimAutoStart,
	releaseAutoStart,
	shouldAutoStart,
	type AutoStartFacts
} from './autoStart.js';

const ready: AutoStartFacts = {
	loaded: true,
	missingContent: true,
	stateAllows: true,
	busy: false,
	failed: false,
	keyReady: true
};

describe('shouldAutoStart', () => {
	it('starts when the content is missing and everything is fine', () => {
		expect(shouldAutoStart(ready)).toBe(true);
	});

	it.each([
		['not loaded yet', { loaded: false }],
		['content exists', { missingContent: false }],
		['wrong state', { stateAllows: false }],
		['already running', { busy: true }],
		['failed before: no loop', { failed: true }],
		['no planning key', { keyReady: false }],
		['key state unknown', { keyReady: null }]
	])('does not start when %s', (_name, change) => {
		expect(shouldAutoStart({ ...ready, ...change })).toBe(false);
	});
});

describe('claimAutoStart', () => {
	it('lets one start through per project and step until released', () => {
		expect(claimAutoStart('p1', 'questions')).toBe(true);
		expect(claimAutoStart('p1', 'questions')).toBe(false);
		expect(claimAutoStart('p1', 'requirements')).toBe(true);
		releaseAutoStart('p1', 'questions');
		expect(claimAutoStart('p1', 'questions')).toBe(true);
	});
});
