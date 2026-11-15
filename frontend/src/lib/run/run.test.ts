import { describe, expect, it } from 'vitest';
import type { Run, RunEvent } from '#lib/api/client.js';
import {
	MAX_CONSOLE_LINES,
	atCostLimit,
	canDownload,
	canResume,
	canStop,
	canStart,
	formatMinutes,
	formatUsd,
	hasQuestion,
	isActive,
	lastSeen,
	limitHasRoom,
	mergeConsole,
	oldestSeq,
	prependOlder,
	progress,
	suggestedLimit
} from './run.js';

const step = (status: 'waiting' | 'running' | 'done' | 'failed') => ({
	id: status,
	seq: 1,
	key: 'plan',
	title: 'Plan',
	status,
	costUsd: 0,
	tokens: 0,
	turns: 0,
	seconds: 0,
	attempts: 0
});

const run = (status: Run['status']): Run => ({
	id: 'r1',
	projectId: 'p1',
	contractVersion: 1,
	harness: 'claude_code',
	status,
	resumeCount: 0,
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
		for (const status of ['waiting', 'done', 'interrupted'] as const) {
			expect(isActive(run(status))).toBe(false);
		}
		expect(isActive({ ...run('waiting'), waitKind: 'tokens' })).toBe(true);
		expect(isActive({ ...run('waiting'), waitKind: 'cost_limit' })).toBe(false);
	});

	it('shows the question only while the run waits for the client', () => {
		const asking: Run = {
			...run('waiting'),
			waitKind: 'question',
			question: { id: 'q1', text: 'What now?', stepTitle: 'Build', options: ['retry', 'skip'] }
		};
		expect(hasQuestion(asking)).toBe(true);
		expect(hasQuestion({ ...asking, waitKind: 'cost_limit' })).toBe(false);
		expect(hasQuestion(run('executing'))).toBe(false);
		expect(hasQuestion(null)).toBe(false);
	});

	it('lets the client stop a run that is not over, and resume only an interrupted one', () => {
		for (const status of ['preparing', 'executing', 'waiting', 'interrupted'] as const) {
			expect(canStop(run(status))).toBe(true);
		}
		for (const status of ['done', 'failed'] as const) {
			expect(canStop(run(status))).toBe(false);
		}
		expect(canStop(null)).toBe(false);
		expect(canResume(run('interrupted'))).toBe(true);
		expect(canResume(run('waiting'))).toBe(false);
		expect(canResume({ ...run('waiting'), waitKind: 'tokens' })).toBe(true);
		expect(canResume({ ...run('waiting'), waitKind: 'cost_limit' })).toBe(true);
		expect(canResume({ ...run('waiting'), waitKind: 'question' })).toBe(false);
		expect(canResume(run('failed'))).toBe(false);
	});

	it('knows when a new cost limit leaves room', () => {
		const atLimit: Run = { ...run('waiting'), waitKind: 'cost_limit', costUsd: 9.9 };
		expect(atCostLimit(atLimit)).toBe(true);
		expect(atCostLimit(run('waiting'))).toBe(false);
		expect(limitHasRoom(atLimit, 10)).toBe(false);
		expect(limitHasRoom(atLimit, 10.2)).toBe(true);
		expect(limitHasRoom(atLimit, Number.NaN)).toBe(false);
		expect(suggestedLimit(atLimit)).toBe(20);
	});

	it('does not offer a new Run while the run waits', () => {
		expect(canStart('executing', run('waiting'))).toBe(false);
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
		expect(canDownload(run('waiting'))).toBe(true);
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

describe('report figures', () => {
	it('formats seconds as minutes with one decimal', () => {
		expect(formatMinutes(600)).toBe('10.0');
		expect(formatMinutes(90)).toBe('1.5');
		expect(formatMinutes(0)).toBe('0.0');
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

	it('keeps app output apart from console lines', () => {
		const app = { ...event(5), kind: 'app' as const };
		expect(mergeConsole([], [event(4), app], 'app')).toEqual([app]);
		expect(mergeConsole([], [event(4), app])).toEqual([event(4)]);
	});

	it('puts older lines in front without doubles', () => {
		const shown = [event(5), event(6)];
		const merged = prependOlder(shown, [event(3), event(4), event(5)]);
		expect(merged.map((line) => line.seq)).toEqual([3, 4, 5, 6]);
		expect(oldestSeq(merged)).toBe(3);
		expect(oldestSeq([])).toBeNull();
	});

	it('finds the number to continue after', () => {
		expect(lastSeen([event(4), event(9), event(5, 'run')], 2)).toBe(9);
		expect(lastSeen([], 7)).toBe(7);
	});
});
