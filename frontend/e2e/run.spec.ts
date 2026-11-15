import { expect, test, type Page } from '@playwright/test';

test('confirmed contract to a finished run and its ZIP, surviving a reload', async ({ page }) => {
	await page.goto('/#new');
	await page.getByTestId('wish').fill('A canteen app where students order a meal and pick it up');
	await page.getByTestId('project-key').fill('sk-any');
	await page.getByTestId('create').click();
	await page.getByTestId('ask').click();
	await page.getByTestId('save-answers').click();
	await expect(page.getByTestId('answers-saved')).toBeVisible();
	await page.getByTestId('write-requirements').click();
	await expect(page.getByTestId('step-requirements')).toContainText('FR-001');
	await page.getByTestId('generate').click();

	// Run is offered only once the contract is confirmed
	await expect(page.getByTestId('start-run')).toHaveCount(0);
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');

	// US-06.03/04/07: start, watch tasks, steps, cost and console, reload during or after the run
	await page.getByTestId('start-run').click();
	await expect(page.getByTestId('run-card')).toBeVisible();
	await page.reload();
	await expect(page.getByTestId('run-card')).toBeVisible();
	await expect(page.getByTestId('run-status')).toHaveText('Finished', { timeout: 60_000 });
	await expect(page.getByTestId('run-progress')).toContainText('8 of 8 steps done');
	await expect(page.getByTestId('run-task-1')).toContainText('Order a meal');
	await expect(page.getByTestId('run-task-2')).toContainText('Notify the student');
	await expect(page.getByTestId('run-cost')).toContainText('Cost so far: USD 0.08 of USD 10.00');
	await expect(page.getByTestId('run-console')).toContainText(
		'Independent check: Stub: step check done'
	);
	await expect(page.getByTestId('start-run')).toHaveText('Run again');

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
	await page.goto('/#new');
	await page.getByTestId('wish').fill(wish);
	await page.getByTestId('project-key').fill('sk-any');
	await page.getByTestId('create').click();
	await page.getByTestId('ask').click();
	await page.getByTestId('save-answers').click();
	await expect(page.getByTestId('answers-saved')).toBeVisible();
	await page.getByTestId('write-requirements').click();
	await expect(page.getByTestId('step-requirements')).toContainText('FR-001');
	await page.getByTestId('generate').click();
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');
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
