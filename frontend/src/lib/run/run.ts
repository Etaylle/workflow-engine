import type { Run, RunEvent } from '#lib/api/client.js';

/** Console lines kept in the browser; older ones are fetched on demand. */
export const MAX_CONSOLE_LINES = 1000;

/** Lines fetched per page when scrolling back. */
export const OLDER_PAGE = 300;

/** A run that is still being built: the screen follows it live. */
export const isActive = (run: Run | null): boolean =>
	run !== null &&
	(run.status === 'preparing' ||
		run.status === 'executing' ||
		(run.status === 'waiting' && run.waitKind === 'tokens'));

/** The client may stop a run that is not over: building, waiting or interrupted. */
export const canStop = (run: Run | null): boolean =>
	run !== null && ['preparing', 'executing', 'waiting', 'interrupted'].includes(run.status);

/**
 * An interrupted run continues from the last checkpoint once the key is entered again; a run that
 * waits for tokens or at its cost limit is continued by the client too.
 */
export const canResume = (run: Run | null): boolean =>
	run !== null &&
	(run.status === 'interrupted' ||
		(run.status === 'waiting' && (run.waitKind === 'tokens' || run.waitKind === 'cost_limit')));

/** The run waits at its cost limit and asks for a higher one. */
export const atCostLimit = (run: Run | null): boolean =>
	run !== null && run.status === 'waiting' && run.waitKind === 'cost_limit';

/** A limit that leaves room to continue: what was spent plus the least budget of one step. */
export const MIN_ROOM_CENTS = 30;
export const suggestedLimit = (run: Run): number => Math.ceil((run.costLimitUsd * 2) / 5) * 5;
export const limitHasRoom = (run: Run, limit: number): boolean =>
	Number.isFinite(limit) &&
	Math.round(limit * 100) >= Math.round(run.costUsd * 100) + MIN_ROOM_CENTS;

/** The run waits for an answer of the client: a defect that is not fixable. */
export const hasQuestion = (run: Run | null): boolean =>
	run !== null && run.status === 'waiting' && run.waitKind === 'question' && !!run.question;

/** The Run button shows when the contract is confirmed (or built before) and no run is under way. */
export function canStart(projectStatus: string, run: Run | null): boolean {
	return (
		['confirmed', 'executing', 'done'].includes(projectStatus) &&
		!isActive(run) &&
		run?.status !== 'waiting'
	);
}

/** The ZIP is offered once the run has stopped building and has at least a workspace. */
export const canDownload = (run: Run | null): boolean => run !== null && !isActive(run);

/** Steps done and steps in all tasks, for the progress line. */
export function progress(run: Run): { done: number; total: number } {
	const steps = run.tasks.flatMap((task) => task.steps);
	return { done: steps.filter((step) => step.status === 'done').length, total: steps.length };
}

/**
 * Adds the events of one kind not yet shown (by number), oldest first, keeping the last lines.
 * Console lines and the output of the generated app are both lines of the event stream.
 */
export function mergeConsole(
	lines: RunEvent[],
	incoming: RunEvent[],
	kind: RunEvent['kind'] = 'console'
): RunEvent[] {
	const known = new Set(lines.map((line) => line.seq));
	const added = incoming.filter((event) => event.kind === kind && !known.has(event.seq));
	return [...lines, ...added].sort((a, b) => a.seq - b.seq).slice(-MAX_CONSOLE_LINES);
}

/** Puts a page of older lines in front of the lines shown (no doubles, oldest first). */
export function prependOlder(lines: RunEvent[], older: RunEvent[]): RunEvent[] {
	const known = new Set(lines.map((line) => line.seq));
	return [...older.filter((event) => !known.has(event.seq)), ...lines].sort(
		(a, b) => a.seq - b.seq
	);
}

/** The number of the oldest line shown, or null when there is none. */
export const oldestSeq = (lines: RunEvent[]): number | null => lines[0]?.seq ?? null;

/** The highest event number seen, so a reconnect continues after it. */
export const lastSeen = (events: RunEvent[], from: number): number =>
	events.reduce((max, event) => Math.max(max, event.seq), from);

export const formatUsd = (usd: number): string => usd.toFixed(2);

/** Minutes with one decimal, for the report: 600 seconds are "10.0". */
export const formatMinutes = (seconds: number): string => (seconds / 60).toFixed(1);
