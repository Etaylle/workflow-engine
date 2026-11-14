import { expect, test } from '@playwright/test';

test('wish to questions, requirements, workflow and confirmed contract, surviving reloads', async ({
	page
}) => {
	await page.goto('/#new');

	await page.getByTestId('wish').fill('A canteen app where students order a meal and pick it up');
	await page.getByTestId('project-key').fill('sk-any');
	await page.getByTestId('create').click();

	// Screen 1 and 2: brief and questions with the proposal preselected
	await page.getByTestId('ask').click();
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
	await expect(page.getByTestId('question-4')).toHaveCount(0);
	await page.getByTestId('question-3').locator('textarea').fill('45 minutes');
	await page.getByTestId('save-answers').click();
	await expect(page.getByTestId('answers-saved')).toBeVisible();
	await expect(page.getByTestId('decided-1')).toContainText('AI proposal');
	await expect(page.getByTestId('decided-3')).toContainText('by you');

	// Reload: the answers are still there and the next step is open
	await page.reload();
	await expect(page.getByTestId('question-3').locator('textarea')).toHaveValue('45 minutes');
	await expect(page.getByTestId('write-requirements')).toBeVisible();

	// Screen 3: requirements
	await page.getByTestId('write-requirements').click();
	await expect(page.getByTestId('step-requirements')).toContainText('FR-001');
	await page.reload();
	await expect(page.getByTestId('step-requirements')).toContainText('FR-001');

	// Screen 4: workflow, then screen 8: review and confirm
	await page.getByTestId('generate').click();
	const diagram = page.getByTestId('diagram');
	await expect(diagram.getByText('Order a meal')).toBeVisible();
	await expect(diagram.getByText('Prepare the meal')).toBeVisible();
	await expect(diagram.locator('.svelte-flow__edge')).toHaveCount(5);
	await page.waitForTimeout(500);
	await diagram.screenshot({ path: 'test-results/workflow-diagram.png' });
	await expect(page.getByTestId('review-summary')).toContainText('3 answers (2 by AI proposal)');

	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');

	await page.reload();
	await expect(page.getByTestId('diagram').getByText('Order a meal')).toBeVisible();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');
	await expect(page.getByTestId('question-1')).toContainText('How do students pay?');

	await page.getByTestId('generate').click();
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('versions')).toContainText('1, 2');

	await page.goto('/#projects');
	await expect(page.getByTestId('projects')).toContainText('A canteen app');
	await expect(page.getByTestId('projects')).toContainText('Confirmed');

	// US-01.08: editing the wish moves the project back to the first step
	await page.goto('/#projects');
	await page.getByTestId('project-row').first().getByRole('link', { name: 'Open' }).click();
	await page.getByTestId('wish-edit-open').click();
	await page.getByTestId('wish-edit').fill('A canteen app with a weekly menu');
	await page.getByTestId('wish-save').click();
	await expect(page.getByTestId('wish-text')).toContainText('weekly menu');
	await page.getByTestId('ask').click();
	await expect(page.getByTestId('question-1')).toContainText('How do students pay?');
	await page.reload();
	await expect(page.getByTestId('wish-text')).toContainText('weekly menu');
});
