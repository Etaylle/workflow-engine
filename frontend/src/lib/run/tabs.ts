import type { Run } from '#lib/api/client.js';

export type RunTab = 'activity' | 'files' | 'app';

/** The tabs in order; Files once a step is done, App output while a preview exists. */
export function availableTabs(run: Run | null, previewShown: boolean): RunTab[] {
	const tabs: RunTab[] = ['activity'];
	if (run && run.tasks.some((task) => task.steps.some((step) => step.status === 'done'))) {
		tabs.push('files');
	}
	if (previewShown) tabs.push('app');
	return tabs;
}

/** The chosen tab when it still exists, else the first one (Activity). */
export const effectiveTab = (chosen: RunTab, tabs: RunTab[]): RunTab =>
	tabs.includes(chosen) ? chosen : (tabs[0] ?? 'activity');

/** Preview is offered while at least one task is done. */
export const canPreview = (run: Run | null): boolean =>
	run !== null && run.tasks.some((task) => task.status === 'done');
