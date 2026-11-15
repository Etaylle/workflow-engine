import { expect, test } from '@playwright/test';

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

	// US-06.12: the ZIP of the workspace
	const download = page.waitForEvent('download');
	await page.getByTestId('run-zip').click();
	expect((await download).suggestedFilename()).toMatch(/^run-[0-9a-f]{8}\.zip$/);

	await page.reload();
	await expect(page.getByTestId('run-status')).toHaveText('Finished');
	await page.goto('/#projects');
	await expect(page.getByTestId('projects')).toContainText('Done');
});
