import { describe, expect, it } from 'vitest';
import type { Preview } from '#lib/api/client.js';
import { appUrl, minutesLeft, previewActive, previewShown } from './preview.js';

const running: Preview = {
	status: 'running',
	port: 51234,
	expiresAt: '2026-11-15T10:15:00Z'
};

describe('preview state', () => {
	it('shows a preview unless there is none', () => {
		expect(previewShown(null)).toBe(false);
		expect(previewShown({ status: 'none' })).toBe(false);
		expect(previewShown({ status: 'failed', message: 'x' })).toBe(true);
	});

	it('is active while starting or running', () => {
		expect(previewActive({ status: 'starting' })).toBe(true);
		expect(previewActive(running)).toBe(true);
		expect(previewActive({ status: 'stopped' })).toBe(false);
		expect(previewActive(null)).toBe(false);
	});

	it('links to the app only while it runs', () => {
		expect(appUrl(running, 'localhost')).toBe('http://localhost:51234/');
		expect(appUrl({ status: 'starting', port: 51234 }, 'localhost')).toBeNull();
		expect(appUrl(null, 'localhost')).toBeNull();
	});

	it('counts the minutes left, rounded up, never below zero', () => {
		const at = Date.parse('2026-11-15T10:10:30Z');
		expect(minutesLeft(running, at)).toBe(5);
		expect(minutesLeft(running, Date.parse('2026-11-15T10:20:00Z'))).toBe(0);
		expect(minutesLeft({ status: 'starting' }, at)).toBeNull();
	});
});
