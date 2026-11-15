import { expect, type Page } from '@playwright/test';

/** Creates a project from a wish and walks the planning steps to the review view. */
export async function planToReview(
	page: Page,
	wish: string,
	buildMode: 'whole' | 'tasks' = 'tasks'
): Promise<void> {
	await page.goto('/#new');
	await page.getByTestId('wish').fill(wish);
	await page.getByTestId('project-key').fill('sk-any');
	await page.getByTestId('create').click();
	await page.getByTestId('ask').click();
	await page.getByTestId('save-answers').click();
	await expect(page.getByTestId('step-requirements')).toBeVisible();
	await page.getByTestId('write-requirements').click();
	await expect(page.getByTestId('step-requirements')).toContainText('FR-001');
	await page.getByTestId('requirements-continue').click();
	await page.getByTestId('generate').click();
	await page.getByTestId('workflow-continue').click();
	await expect(page.getByTestId('step-review')).toBeVisible();
	// The default of a small contract is "whole"; most journeys build task by task
	await expect(page.getByTestId('build-mode-whole')).toBeChecked();
	if (buildMode === 'tasks') {
		await page.getByTestId('build-mode-tasks').click();
		await expect(page.getByText('Build mode saved.')).toBeVisible();
	}
}
