import { expect, test } from '@playwright/test';

test('status, language switch and key test with the stub', async ({ page }) => {
	await page.goto('/');
	await expect(page.getByTestId('status')).toContainText('workflow-engine');
	await expect(page.getByRole('heading', { name: 'All projects' })).toBeVisible();

	await page.getByTestId('language').selectOption('de');
	await expect(page.getByRole('heading', { name: 'Alle Projekte' })).toBeVisible();
	await page.getByTestId('language').selectOption('en');

	await page.getByTestId('new-project').click();
	await expect(page.getByRole('heading', { name: 'Describe your idea' })).toBeVisible();
	await page.getByTestId('project-key').fill('bad');
	await page.getByTestId('test-key').click();
	await expect(page.getByTestId('key-result')).toContainText('Check failed');

	await page.getByTestId('project-key').fill('sk-any');
	await page.getByTestId('test-key').click();
	await expect(page.getByTestId('key-result')).toContainText('Key works');
});

test('rename and delete a project from the list', async ({ page }) => {
	await page.goto('/#new');
	await page.getByTestId('name').fill('Delete me');
	await page.getByTestId('wish').fill('A throwaway app');
	await page.getByTestId('create').click();
	await expect(page.getByRole('heading', { name: 'Delete me' })).toBeVisible();

	await page.getByRole('link', { name: '← All projects' }).click();
	const row = page.getByTestId('project-row').first();
	await expect(row).toContainText('Delete me');
	await expect(row).toContainText('Draft');
	await row.getByTestId('rename').click();
	await row.getByTestId('rename-input').fill('Renamed app');
	await row.getByRole('button', { name: 'Save' }).click();
	const renamed = page.getByTestId('project-row').filter({ hasText: 'Renamed app' });
	await expect(renamed).toBeVisible();

	await renamed.getByTestId('delete').click();
	await renamed.getByTestId('confirm-delete').click();
	await expect(page.getByText('Renamed app')).toHaveCount(0);
	await page.reload();
	await expect(page.getByText('Renamed app')).toHaveCount(0);
});

test('the key is changed from the header on a later step and the action is retried', async ({
	page
}) => {
	await page.goto('/#new');
	await expect(page.getByTestId('demo-badge')).toContainText('Demo mode');
	await page.getByTestId('wish').fill('A key journey app');
	await page.getByTestId('project-key').fill('sk-first-key-0001');
	await page.getByTestId('create').click();
	await expect(page.getByTestId('key-button')).toContainText('Key set (…0001)');

	// the key is removed in the header: the next AI action says why it fails
	await page.getByTestId('key-button').click();
	await page.getByTestId('key-remove').click();
	await expect(page.getByTestId('key-button')).toContainText('No key');
	await page.getByTestId('key-close').click();
	await page.getByTestId('ask').click();
	await expect(page.getByTestId('ask-error')).toContainText('No Anthropic key');

	// the error offers the key panel; saving a new key lets the same button work
	await page.getByTestId('open-key-panel').click();
	await page.getByTestId('key-input').fill('sk-second-key-0002');
	await page.getByTestId('key-save').click();
	await expect(page.getByTestId('key-button')).toContainText('Key set (…0002)');
	await page.getByTestId('key-close').click();
	await page.getByTestId('ask').click();
	await expect(page.getByTestId('question-1')).toBeVisible();

	// Test checks a key with the AI; the stub rejects "bad" and a failed check discards the key
	await page.getByTestId('key-button').click();
	await page.getByTestId('key-input').fill('bad');
	await page.getByTestId('key-test').click();
	await expect(page.getByTestId('key-panel-result')).toContainText('Check failed');
	await expect(page.getByTestId('key-button')).toContainText('No key');
});
