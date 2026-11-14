import { describe, expect, it } from 'vitest';
import { relativeTime } from './relativeTime.js';

const now = new Date('2026-11-15T12:00:00Z');

describe('relativeTime', () => {
	it('says now for the same moment', () => {
		expect(relativeTime('2026-11-15T12:00:00Z', now, 'en')).toBe('now');
	});
	it('counts minutes, hours and days', () => {
		expect(relativeTime('2026-11-15T11:46:00Z', now, 'en')).toBe('14 minutes ago');
		expect(relativeTime('2026-11-15T09:00:00Z', now, 'en')).toBe('3 hours ago');
		expect(relativeTime('2026-11-14T12:00:00Z', now, 'en')).toBe('yesterday');
		expect(relativeTime('2026-11-12T12:00:00Z', now, 'en')).toBe('3 days ago');
	});
	it('follows the language', () => {
		expect(relativeTime('2026-11-14T12:00:00Z', now, 'de')).toBe('gestern');
	});
});
