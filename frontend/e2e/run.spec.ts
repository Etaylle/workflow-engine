import { expect, test, type Page } from '@playwright/test';
import { planToReview } from './helpers.js';

test('confirmed contract to a finished run and its ZIP, surviving a reload', async ({ page }) => {
	await planToReview(page, 'A canteen app where students order a meal and pick it up');

	// Run is offered only once the contract is confirmed
	await expect(page.getByTestId('start-run')).toHaveCount(0);
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');
	await page.getByTestId('review-continue').click();

	// Build with: both builders are shown and ready in demo mode; choosing one is saved
	await expect(page.getByTestId('state-claude_code')).toHaveText('Ready');
	await page.getByTestId('pick-gemini_cli').click();
	await expect(page.getByTestId('pick-gemini_cli')).toHaveAttribute('aria-checked', 'true');
	await page.reload();
	await expect(page.getByTestId('pick-gemini_cli')).toHaveAttribute('aria-checked', 'true');
	await page.getByTestId('pick-codex_cli').click();
	await expect(page.getByTestId('pick-codex_cli')).toHaveAttribute('aria-checked', 'true');
	await expect(page.getByTestId('plan-note')).toContainText('ChatGPT sign-in');
	await expect(page.getByTestId('builder-effort').locator('option')).toHaveText([
		'Default',
		'low',
		'medium',
		'high',
		'xhigh'
	]);
	await page.getByTestId('builder-model').fill('gpt-6-luna');
	await page.getByTestId('builder-model').press('Enter');
	await expect(page.getByText('Builder settings saved.')).toBeVisible();
	await page.reload();
	await expect(page.getByTestId('builder-model')).toHaveValue('gpt-6-luna');
	await page.getByTestId('pick-claude_code').click();
	await expect(page.getByTestId('pick-claude_code')).toHaveAttribute('aria-checked', 'true');
	// Model and effort: Claude Code has both, Gemini CLI only a model; saved on change, shown on Review
	await expect(page.getByTestId('builder-effort')).toBeVisible();
	await page.getByTestId('builder-model').selectOption('opus');
	await expect(page.getByText('Builder settings saved.')).toBeVisible();
	await page.getByTestId('builder-effort').selectOption('high');
	await page.getByTestId('step-back').click();
	await expect(page.getByTestId('review-builder')).toContainText('Claude Code · opus · high');
	await page.getByTestId('review-continue').click();
	await expect(page.getByTestId('builder-model')).toHaveValue('opus');
	await expect(page.getByTestId('builder-effort')).toHaveValue('high');
	await page.getByTestId('check-again').click();
	await expect(page.getByTestId('state-gemini_cli')).toHaveText('Ready');

	// US-06.03/04/07: start, watch tasks, steps, cost and console, reload during or after the run
	await page.getByTestId('start-run').click();
	await expect(page.getByTestId('run-card')).toBeVisible();
	await page.reload();
	await expect(page.getByTestId('run-card')).toBeVisible();
	await expect(page.getByTestId('run-status')).toHaveText('Finished', { timeout: 60_000 });
	await expect(page.getByTestId('run-progress')).toContainText('8 of 8 steps done');
	await expect(page.getByTestId('run-task-1')).toContainText('Order a meal');
	await expect(page.getByTestId('run-task-2')).toContainText('Notify the student');
	await expect(page.getByTestId('run-builder')).toHaveText('Built with Claude Code · opus · high');
	await expect(page.getByTestId('run-cost')).toContainText('Cost so far: USD 0.08 of USD 10.00');
	await expect(page.getByTestId('run-console')).toContainText(
		'Independent check: Stub: step check done'
	);
	await expect(page.getByTestId('start-run')).toHaveText('Run again');

	// Activity: what the builder did shows as short lines; scrolling can be paused
	await expect(page.getByTestId('run-console')).toContainText('writes docs/STUB-plan.md');
	await expect(page.getByTestId('run-console')).toContainText('check: CHECK PASSED (stub)');
	await page.getByTestId('run-console-pause').click();
	await expect(page.getByTestId('run-console-pause')).toHaveText('Follow output');

	// Files: what was built, checkpoint by checkpoint, and the text of a file
	await page.getByTestId('tab-files').click();
	await expect(page.getByTestId('files-changes')).toBeVisible();
	await page.getByTestId('files-mode-all').click();
	await page.getByTestId('tree-file').filter({ hasText: 'CONTRACT.md' }).click();
	await expect(page.getByTestId('file-content')).toContainText('Notify the student');
	await page.getByTestId('tab-activity').click();

	// Preview: the app of the last finished step runs on its own; its console output is visible
	await page.getByTestId('start-preview').click();
	await expect(page.getByTestId('preview-status')).toHaveText('Running', { timeout: 30_000 });
	await expect(page.getByTestId('app-console')).toContainText('<(o )___');
	await expect(page.getByTestId('open-app')).toHaveAttribute('href', /^http:\/\/[^/]+:\d+\/$/);
	await page.getByTestId('stop-preview').click();
	await expect(page.getByTestId('preview-status')).toHaveText('Stopped');
	await expect(page.getByTestId('open-app')).toHaveCount(0);
	await page.getByTestId('tab-activity').click();

	// Documents take no room: one quiet button opens them in a dialog
	await expect(page.getByTestId('document-view')).toHaveCount(0);
	await page.getByTestId('open-documents').click();
	await page.getByTestId('view-requirements').click();
	await expect(page.getByTestId('document-view')).toContainText('FR-001');
	await page.getByTestId('close-documents').click();

	// US-06.12: the final report, then the ZIP of the workspace
	await expect(page.getByTestId('report-tasks')).toHaveText('2 of 2 tasks done');
	await expect(page.getByTestId('report-steps')).toHaveText('8 of 8 steps done');
	await expect(page.getByTestId('report-checkpoints')).toContainText('8 checkpoints');
	await expect(page.getByTestId('report-cost')).toContainText('USD 0.08 of USD 10.00');
	await expect(page.getByTestId('report-no-questions')).toBeVisible();
	const download = page.waitForEvent('download');
	await page.getByTestId('run-zip').click();
	expect((await download).suggestedFilename()).toMatch(/^run-[0-9a-f]{8}\.zip$/);

	await page.reload();
	await expect(page.getByTestId('run-status')).toHaveText('Finished');
	await page.goto('/#projects');
	await expect(page.getByTestId('projects')).toContainText('Done');
});

async function startRunWithWish(page: Page, wish: string) {
	await planToReview(page, wish);
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');
	await page.getByTestId('review-continue').click();
	await page.getByTestId('start-run').click();
}

test('an unfixable defect asks the client; the answer continues the run', async ({ page }) => {
	await startRunWithWish(page, 'A canteen app where students order a meal stub-fail:build:3');

	// US-06.05/06: three attempts fail, then one question; the run asks only now
	await expect(page.getByTestId('run-status')).toHaveText('Waiting', { timeout: 60_000 });
	await expect(page.getByTestId('run-question')).toContainText(
		'could not be fixed after 3 attempts'
	);
	await expect(page.getByTestId('attempts-build')).toHaveText('3 attempts');
	await page.reload();
	await expect(page.getByTestId('run-question')).toBeVisible();

	await page.getByTestId('question-hint').fill('Use the simplest approach');
	await page.getByTestId('answer-retry').click();
	await expect(page.getByTestId('run-status')).toHaveText('Finished', { timeout: 60_000 });
	await expect(page.getByTestId('run-progress')).toContainText('8 of 8 steps done');
	await expect(page.getByTestId('report-questions')).toContainText('Use the simplest approach');
	await expect(page.getByTestId('run-question')).toHaveCount(0);
});

test('the client stops a run after a confirmation; it ends stopped', async ({ page }) => {
	await startRunWithWish(page, 'A canteen app where students order a meal stub-slow:1500');
	await expect(page.getByTestId('run-status')).toHaveText('Building');

	// US-06.09: stop needs a confirmation, and "keep going" changes nothing
	await page.getByTestId('stop-run').click();
	await expect(page.getByTestId('stop-confirm')).toContainText('cannot be continued');
	await page.getByTestId('stop-confirm-no').click();
	await expect(page.getByTestId('stop-confirm')).toHaveCount(0);
	await expect(page.getByTestId('run-status')).toHaveText('Building');

	await page.getByTestId('stop-run').click();
	await page.getByTestId('stop-confirm-yes').click();
	await expect(page.getByTestId('run-status')).toHaveText('Stopped');
	await expect(page.getByTestId('stop-run')).toHaveCount(0);
	await expect(page.getByTestId('run-progress')).not.toContainText('8 of 8');
	await page.reload();
	await expect(page.getByTestId('run-status')).toHaveText('Stopped');
});

test('an interrupted run continues from the last checkpoint', async ({ page }) => {
	await startRunWithWish(page, 'A canteen app where students order a meal stub-crash:build:1');

	// US-06.10: the run is interrupted (the stub crashes once), then resumed
	await expect(page.getByTestId('run-status')).toHaveText('Interrupted', { timeout: 60_000 });
	await expect(page.getByTestId('run-progress')).toContainText('1 of 8 steps done');
	await page.getByTestId('resume-run').click();
	await expect(page.getByTestId('run-status')).toHaveText('Finished', { timeout: 60_000 });
	await expect(page.getByTestId('run-progress')).toContainText('8 of 8 steps done');
	await expect(page.getByTestId('run-console')).toContainText('Independent check');
});

test('a rate limit makes the run wait for tokens and continue by itself', async ({ page }) => {
	await startRunWithWish(page, 'A canteen app where students order a meal stub-limit:build:1');

	// US-06.10: the run does not fail; it waits, says why, and goes on
	await expect(page.getByTestId('run-status')).toHaveText('Waiting', { timeout: 60_000 });
	await expect(page.getByTestId('run-wait')).toContainText('Waiting for tokens');
	await expect(page.getByTestId('run-status')).toHaveText('Finished', { timeout: 60_000 });
	await expect(page.getByTestId('run-progress')).toContainText('8 of 8 steps done');
});

test('a preview can be tried while the run goes on', async ({ page }) => {
	await startRunWithWish(page, 'A canteen app where students order a meal stub-slow:500');
	await expect(page.getByTestId('start-preview')).toHaveCount(0);

	// A task is done after its four steps; the builder carries on in its own folder
	await expect(page.getByTestId('run-task-1')).toHaveAttribute('data-status', 'done', {
		timeout: 30_000
	});
	await expect(page.getByTestId('run-status')).toHaveText('Building');
	await page.getByTestId('start-preview').click();
	await expect(page.getByTestId('preview-status')).toHaveText('Running', { timeout: 30_000 });
	await expect(page.getByTestId('stop-run')).toBeVisible();
	await page.getByTestId('stop-preview').click();
	await expect(page.getByTestId('preview-status')).toHaveText('Stopped');
	await page.getByTestId('stop-run').click();
	await page.getByTestId('stop-confirm-yes').click();
	await expect(page.getByTestId('run-status')).toHaveText('Stopped');
});

test('a console program: chosen on the review, built, its output shown, no link to open', async ({
	page
}) => {
	await planToReview(page, 'Make an app that opens a console and shows a duck');
	await page.getByTestId('template-select').selectOption('node-console');
	await page.reload();
	await expect(page.getByTestId('template-select')).toHaveValue('node-console');

	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toBeVisible();
	await page.getByTestId('review-continue').click();
	await page.getByTestId('start-run').click();
	await expect(page.getByTestId('run-status')).toHaveText('Finished', { timeout: 60_000 });

	await page.getByTestId('start-preview').click();
	await expect(page.getByTestId('preview-status')).toHaveText('Stopped', { timeout: 30_000 });
	await expect(page.getByTestId('app-console')).toContainText('<(o )___');
	await expect(page.getByTestId('open-app')).toHaveCount(0);
});

test('a small contract is built all at once as one unit with its steps', async ({ page }) => {
	await planToReview(page, 'A canteen app where students order a meal and pick it up', 'whole');
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');
	await page.getByTestId('review-continue').click();

	await page.getByTestId('start-run').click();
	await expect(page.getByTestId('run-status')).toHaveText('Finished', { timeout: 60_000 });
	await expect(page.getByTestId('run-progress')).toContainText('4 of 4 steps done');
	await expect(page.getByTestId('run-task-1')).toContainText('Build the whole software');
	await expect(page.getByTestId('run-task-2')).toHaveCount(0);
});
