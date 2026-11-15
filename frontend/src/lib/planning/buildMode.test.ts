import { describe, expect, it } from 'vitest';
import { effectiveBuildMode } from './buildMode.js';

describe('effectiveBuildMode', () => {
	it('builds all at once up to 8 software tasks', () => {
		expect(effectiveBuildMode(null, 1)).toBe('whole');
		expect(effectiveBuildMode(null, 8)).toBe('whole');
	});

	it('builds task by task above 8 software tasks', () => {
		expect(effectiveBuildMode(null, 9)).toBe('tasks');
	});

	it('keeps an explicit choice whatever the count', () => {
		expect(effectiveBuildMode('tasks', 2)).toBe('tasks');
		expect(effectiveBuildMode('whole', 20)).toBe('whole');
	});
});
