import { expect, type Page } from '@playwright/test';

/** Gives the engine a planning key for the session (the stub accepts any text but "bad"). */
export async function setPlanningKey(page: Page, key = 'sk-any'): Promise<void> {
	const response = await page.request.put('/api/ai/keys/anthropic', { data: { apiKey: key } });
	expect(response.ok()).toBe(true);
}

/** Creates a project from a wish and walks the planning steps to the review view. */
export async function planToReview(
	page: Page,
	wish: string,
	buildMode: 'whole' | 'tasks' = 'tasks'
): Promise<void> {
	await setPlanningKey(page);
	await page.goto('/#new');
	await page.getByTestId('wish').fill(wish);
	await page.getByTestId('create').click();
	await expect(page.getByTestId('question-1')).toBeVisible();
	await page.getByTestId('save-answers').click();
	await expect(page.getByTestId('step-requirements')).toBeVisible();
	await expect(page.getByTestId('step-requirements')).toContainText('FR-001');
	await page.getByTestId('requirements-continue').click();
	await expect(page.getByTestId('workflow-continue')).toBeEnabled();
	await page.getByTestId('workflow-continue').click();
	await expect(page.getByTestId('step-review')).toBeVisible();
	// The default of a small contract is "whole"; most journeys build task by task
	await expect(page.getByTestId('build-mode-whole')).toBeChecked();
	if (buildMode === 'tasks') {
		await page.getByTestId('build-mode-tasks').click();
		await expect(page.getByText('Build mode saved.')).toBeVisible();
	}
}
