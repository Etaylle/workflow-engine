import type { Run, RunEvent } from '#lib/api/client.js';

/** Console lines kept on the screen. */
export const MAX_CONSOLE_LINES = 200;

/** A run that is still being built: the screen follows it live. */
export const isActive = (run: Run | null): boolean =>
	run !== null && (run.status === 'preparing' || run.status === 'executing');

/** The Run button shows when the contract is confirmed (or built before) and no run is active. */
export function canStart(projectStatus: string, run: Run | null): boolean {
	return ['confirmed', 'executing', 'done'].includes(projectStatus) && !isActive(run);
}

/** The ZIP is offered once the run has stopped building and has at least a workspace. */
export const canDownload = (run: Run | null): boolean => run !== null && !isActive(run);

/** Steps done and steps in all tasks, for the progress line. */
export function progress(run: Run): { done: number; total: number } {
	const steps = run.tasks.flatMap((task) => task.steps);
	return { done: steps.filter((step) => step.status === 'done').length, total: steps.length };
}

/** Adds the console events not yet shown (by number), oldest first, keeping the last lines. */
export function mergeConsole(lines: RunEvent[], incoming: RunEvent[]): RunEvent[] {
	const known = new Set(lines.map((line) => line.seq));
	const added = incoming.filter((event) => event.kind === 'console' && !known.has(event.seq));
	return [...lines, ...added].sort((a, b) => a.seq - b.seq).slice(-MAX_CONSOLE_LINES);
}

/** The highest event number seen, so a reconnect continues after it. */
export const lastSeen = (events: RunEvent[], from: number): number =>
	events.reduce((max, event) => Math.max(max, event.seq), from);

export const formatUsd = (usd: number): string => usd.toFixed(2);
