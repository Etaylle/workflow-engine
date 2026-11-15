import { describe, expect, it } from 'vitest';
import type { Run, RunEvent } from '#lib/api/client.js';
import {
	MAX_CONSOLE_LINES,
	canDownload,
	canStart,
	formatUsd,
	isActive,
	lastSeen,
	mergeConsole,
	progress
} from './run.js';

const step = (status: 'waiting' | 'running' | 'done' | 'failed') => ({
	id: status,
	seq: 1,
	key: 'plan',
	title: 'Plan',
	status,
	costUsd: 0,
	turns: 0,
	seconds: 0,
	attempts: 0
});

const run = (status: Run['status']): Run => ({
	id: 'r1',
	projectId: 'p1',
	contractVersion: 1,
	status,
	costUsd: 0.5,
	costLimitUsd: 10,
	lastSeq: 3,
	startedAt: '2026-11-15T10:00:00Z',
	tasks: [
		{ id: 't1', seq: 1, title: 'Order', status: 'running', steps: [step('done'), step('running')] },
		{ id: 't2', seq: 2, title: 'Notify', status: 'pending', steps: [step('waiting')] }
	]
});

const event = (seq: number, kind: RunEvent['kind'] = 'console'): RunEvent => ({
	seq,
	kind,
	text: `line ${seq}`,
	at: '2026-11-15T10:00:00Z'
});

describe('run state', () => {
	it('knows which runs are still being built', () => {
		expect(isActive(null)).toBe(false);
		expect(isActive(run('preparing'))).toBe(true);
		expect(isActive(run('executing'))).toBe(true);
		for (const status of ['paused', 'done', 'interrupted'] as const) {
			expect(isActive(run(status))).toBe(false);
		}
	});

	it('offers Run only on a confirmed contract without an active run', () => {
		expect(canStart('confirmed', null)).toBe(true);
		expect(canStart('workflow_drafted', null)).toBe(false);
		expect(canStart('executing', run('executing'))).toBe(false);
		expect(canStart('executing', run('interrupted'))).toBe(true);
		expect(canStart('done', run('done'))).toBe(true);
	});

	it('offers the ZIP once the run has stopped', () => {
		expect(canDownload(null)).toBe(false);
		expect(canDownload(run('executing'))).toBe(false);
		expect(canDownload(run('paused'))).toBe(true);
		expect(canDownload(run('done'))).toBe(true);
	});

	it('counts the finished steps over all tasks', () => {
		expect(progress(run('executing'))).toEqual({ done: 1, total: 3 });
	});

	it('formats the cost with two decimals', () => {
		expect(formatUsd(0.5)).toBe('0.50');
		expect(formatUsd(1.1049)).toBe('1.10');
	});
});

describe('console merge', () => {
	it('keeps only console events, without doubles, in order', () => {
		const first = mergeConsole([], [event(2), event(1), event(3, 'step')]);
		const again = mergeConsole(first, [event(2), event(4)]);
		expect(again.map((line) => line.seq)).toEqual([1, 2, 4]);
	});

	it('keeps the last lines only', () => {
		const many = Array.from({ length: MAX_CONSOLE_LINES + 20 }, (_, i) => event(i + 1));
		const merged = mergeConsole([], many);
		expect(merged).toHaveLength(MAX_CONSOLE_LINES);
		expect(merged[0]?.seq).toBe(21);
	});

	it('finds the number to continue after', () => {
		expect(lastSeen([event(4), event(9), event(5, 'run')], 2)).toBe(9);
		expect(lastSeen([], 7)).toBe(7);
	});
});
