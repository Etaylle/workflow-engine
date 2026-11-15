import { expect, test } from '@playwright/test';
import { planToReview, setPlanningKey } from './helpers.js';

test('wish to questions, requirements, workflow and confirmed contract, surviving reloads', async ({
	page
}) => {
	await setPlanningKey(page);
	await page.goto('/#new');

	await page.getByTestId('wish').fill('A canteen app where students order a meal and pick it up');
	await page.getByTestId('create').click();

	// Screen 1 and 2: brief and questions write themselves, the proposal is preselected
	await expect(page.getByTestId('question-1')).toContainText('How do students pay?');
	await expect(
		page.getByTestId('question-1').getByRole('radio', { name: 'Online at the order' })
	).toBeChecked();
	await expect(page.getByTestId('brief')).toContainText('Brief');
	// US-02.03: edit, add and delete single questions before answering
	await page.getByTestId('edit-3').click();
	await page.getByTestId('editor-text').fill('How long until pickup?');
	await page.getByTestId('editor-save').click();
	await expect(page.getByTestId('question-3')).toContainText('How long until pickup?');
	await page.getByTestId('add-question').click();
	await page.getByTestId('editor-text').fill('Which language?');
	await page.getByTestId('editor-options').fill('English\nGerman');
	await page.getByTestId('editor-proposal').fill('German');
	await page.getByTestId('editor-save').click();
	await expect(page.getByTestId('question-4')).toContainText('Which language?');
	await page.getByTestId('remove-4').click();
	await page.getByTestId('remove-question-yes').click();
	await expect(page.getByTestId('question-4')).toHaveCount(0);
	await page.getByTestId('question-3').locator('textarea').fill('45 minutes');
	await page.getByTestId('save-answers').click();
	await expect(page.getByTestId('step-requirements')).toBeVisible();
	await expect(page.getByTestId('toast').filter({ hasText: 'Answers saved' })).toBeVisible();
	await page.getByTestId('chip-questions').click();
	await expect(page.getByTestId('decided-1')).toContainText('AI proposal');
	await expect(page.getByTestId('decided-3')).toContainText('by you');

	// Reload: the answers are still there and the next step is open
	await page.reload();
	await expect(page.getByTestId('question-3').locator('textarea')).toHaveValue('45 minutes');
	await page.getByTestId('chip-requirements').click();

	// Screen 3: requirements
	await expect(page.getByTestId('step-requirements')).toContainText('FR-001');
	await page.reload();
	await expect(page.getByTestId('step-requirements')).toContainText('FR-001');

	// Screen 4: workflow, then screen 8: review and confirm
	await page.getByTestId('requirements-continue').click();
	const diagram = page.getByTestId('diagram');
	await expect(diagram.getByText('Order a meal')).toBeVisible();
	await expect(diagram.getByText('Prepare the meal')).toBeVisible();
	await expect(diagram.locator('.svelte-flow__edge')).toHaveCount(5);
	await page.waitForTimeout(500);
	await diagram.screenshot({ path: 'test-results/workflow-diagram.png' });
	await page.getByTestId('workflow-continue').click();
	await expect(page.getByTestId('review-summary')).toContainText(
		'3 answers, 2 decided by the AI proposal'
	);

	// US-04.03/04: edit a step, add one after it, delete another with reconnect, save
	await page.getByTestId('edit-workflow').click();
	await page.getByTestId('step-title-3').fill('Choose a meal');
	await page.getByTestId('add-after-2').click();
	await expect(page.getByTestId('step-7')).toBeVisible();
	await page.getByTestId('delete-step-7').click();
	await expect(page.getByTestId('step-7')).toHaveCount(0);
	await page.getByTestId('editor-save-workflow').click();
	await expect(page.getByTestId('diagram').getByText('Choose a meal')).toBeVisible();
	await expect(page.getByTestId('workflow-editor')).toHaveCount(0);
	await page.getByTestId('edit-workflow').click();
	await page.getByTestId('delete-step-6').click();
	await page.getByTestId('editor-save-workflow').click();
	await expect(page.getByTestId('workflow-edit-error')).toContainText('end_event');
	await page.getByRole('button', { name: 'Cancel' }).click();

	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');

	// US-03.02/03: both documents can be read and downloaded
	await expect(page.getByTestId('documents')).toHaveCount(0);
	await page.getByTestId('open-documents').click();
	await page.getByTestId('view-plan').click();
	await expect(page.getByTestId('document-view')).toContainText('Choose a meal');
	await page.getByTestId('view-requirements').click();
	await expect(page.getByTestId('document-view')).toContainText('FR-001');
	const download = page.waitForEvent('download');
	await page.getByTestId('docx-plan').click();
	expect((await download).suggestedFilename()).toBe('plan.docx');
	await page.getByTestId('close-documents').click();
	await expect(page.getByTestId('documents')).toHaveCount(0);

	await page.reload();
	await expect(page.getByTestId('diagram').getByText('Order a meal')).toBeVisible();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');
	await page.getByTestId('chip-questions').click();
	await expect(page.getByTestId('question-1')).toContainText('How do students pay?');

	await page.getByTestId('chip-workflow').click();
	await page.getByTestId('generate-again').click();
	await page.getByTestId('workflow-continue').click();
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('versions')).toContainText('1, 2');

	await page.goto('/#projects');
	await expect(page.getByTestId('projects')).toContainText('A canteen app');
	await expect(page.getByTestId('projects')).toContainText('Confirmed');

	// US-01.08: editing the wish moves the project back to the first step
	await page.goto('/#projects');
	await page.getByTestId('project-row').first().getByRole('link', { name: 'Open' }).click();
	await page.getByTestId('chip-wish').click();
	await page.getByTestId('wish-edit-open').click();
	await page.getByTestId('wish-edit').fill('A canteen app with a weekly menu');
	await page.getByTestId('wish-save').click();
	await expect(page.getByTestId('wish-text')).toContainText('weekly menu');
	await page.reload();
	await expect(page.getByTestId('wish-text')).toContainText('weekly menu');
	await page.getByTestId('wish-continue').click();
	await expect(page.getByTestId('question-1')).toContainText('How do students pay?');
});

test('the page scrolls with the wheel over the diagram; zoom is by buttons', async ({ page }) => {
	await planToReview(page, 'A canteen app where students order a meal and pick it up');
	await page.setViewportSize({ width: 1280, height: 600 });
	const diagram = page.getByTestId('diagram');
	await expect(diagram.getByText('Order a meal')).toBeVisible();
	const box = await diagram.boundingBox();
	if (!box) throw new Error('the diagram has no box');
	await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
	const before = await page.evaluate(() => window.scrollY);
	await page.mouse.wheel(0, 400);
	await expect.poll(() => page.evaluate(() => window.scrollY)).toBeGreaterThan(before);
	await expect(diagram.getByRole('button', { name: 'Fit the diagram to the view' })).toBeVisible();
});
