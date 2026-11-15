import { expect, test } from '@playwright/test';

test('choose and edit a step profile; the run uses it', async ({ page }) => {
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
	await expect(page.getByTestId('review-steps')).toContainText('Short: 4 steps per task');

	// The cost limit per run is chosen on the review page
	await page.getByTestId('limit-input').fill('abc');
	await expect(page.getByTestId('limit-invalid')).toBeVisible();
	await expect(page.getByTestId('limit-save')).toBeDisabled();
	await page.getByTestId('limit-input').fill('7,5');
	await page.getByTestId('limit-save').click();
	await expect(page.getByTestId('limit-saved')).toBeVisible();
	await page.screenshot({ path: 'test-results/review-limits.png', fullPage: true });

	// Screen 7 is optional and reachable from the review table
	await page.getByTestId('review-steps-link').click();
	await expect(page.getByRole('heading', { name: 'Team and steps' })).toBeVisible();
	const shortCard = page.locator('[data-profile="Short"]');
	await expect(shortCard.getByTestId('profile-used')).toBeVisible();
	await expect(
		page.locator('[data-profile="16 steps"]').getByTestId('profile-steps').locator('li')
	).toHaveCount(16);
	await expect(shortCard.getByTestId('profile-edit')).toHaveCount(0);

	// A copy is edited: rename the first step, move it down
	await shortCard.getByTestId('profile-copy').click();
	await shortCard.getByTestId('copy-name').fill('Mine');
	await shortCard.getByTestId('copy-create').click();
	const mine = page.locator('[data-profile="Mine"]');
	await mine.getByTestId('step-title').first().fill('Think first');
	await expect(mine.getByTestId('profile-editor')).toBeVisible();
	await mine.getByRole('button', { name: 'Move down' }).first().click();
	await page.screenshot({ path: 'test-results/steps-edit.png', fullPage: true });
	await mine.getByTestId('profile-save').click();
	await expect(mine.getByTestId('profile-steps').locator('li').nth(1)).toContainText('Think first');
	await mine.getByTestId('profile-use').click();
	await expect(mine.getByTestId('profile-used')).toBeVisible();
	await page.screenshot({ path: 'test-results/steps-page.png', fullPage: true });

	// Back on the review the choice shows; the run uses it
	await page.getByRole('link', { name: '← Back to the project' }).click();
	await expect(page.getByTestId('review-steps')).toContainText('Mine: 4 steps per task');
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');
	await expect(page.getByTestId('limit-input')).toHaveValue('7.5');
	await page.getByTestId('start-run').click();
	await expect(page.getByTestId('run-status')).toHaveText('Finished', { timeout: 60_000 });
	await expect(page.getByTestId('run-task-1')).toContainText('Think first');
	await expect(page.getByTestId('run-cost')).toContainText('of USD 7.50');

	// Clean up: back to the default, delete the copy
	await page.getByTestId('review-steps-link').click();
	await page.locator('[data-profile="Short"]').getByTestId('profile-use').click();
	await page.locator('[data-profile="Mine"]').getByTestId('profile-delete').click();
	await expect(page.locator('[data-profile="Mine"]')).toHaveCount(0);
});
