import { describe, expect, it } from 'vitest';
import type { Run } from '#lib/api/client.js';
import { availableTabs, canPreview, effectiveTab } from './tabs.js';

const run = (taskStatus: string, stepStatus: string): Run =>
	({
		tasks: [{ status: taskStatus, steps: [{ status: stepStatus }] }]
	}) as unknown as Run;

describe('run tabs', () => {
	it('shows only Activity before a step is done', () => {
		expect(availableTabs(null, false)).toEqual(['activity']);
		expect(availableTabs(run('running', 'running'), false)).toEqual(['activity']);
	});

	it('adds Files after a done step and App output while a preview exists', () => {
		expect(availableTabs(run('running', 'done'), false)).toEqual(['activity', 'files']);
		expect(availableTabs(run('done', 'done'), true)).toEqual(['activity', 'files', 'app']);
	});

	it('falls back to the first tab when the chosen one is gone', () => {
		expect(effectiveTab('app', ['activity', 'files'])).toBe('activity');
		expect(effectiveTab('files', ['activity', 'files'])).toBe('files');
	});

	it('offers Preview once a task is done, not before', () => {
		expect(canPreview(run('running', 'done'))).toBe(false);
		expect(canPreview(run('done', 'done'))).toBe(true);
		expect(canPreview(null)).toBe(false);
	});
});
