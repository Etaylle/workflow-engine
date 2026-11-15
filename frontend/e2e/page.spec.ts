import { expect, test } from '@playwright/test';
import { setPlanningKey } from './helpers.js';

test('status, language switch and key test with the stub', async ({ page }) => {
	await page.goto('/');
	await expect(page.getByTestId('status')).toContainText('workflow-engine');
	await expect(page.getByRole('heading', { name: 'All projects' })).toBeVisible();

	await page.getByTestId('language').selectOption('de');
	await expect(page.getByRole('heading', { name: 'Alle Projekte' })).toBeVisible();
	await page.getByTestId('language').selectOption('en');

	await page.getByTestId('new-project').click();
	await expect(page.getByRole('heading', { name: 'Describe your idea' })).toBeVisible();
	await expect(page.getByTestId('planning-line')).toBeVisible();
	await page.getByTestId('open-key-panel').click();
	await expect(page.getByTestId('key-panel')).toBeVisible();
	await page.keyboard.press('Escape');
	await expect(page.getByTestId('key-panel')).toHaveCount(0);
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
	const dialog = page.getByRole('dialog');
	await expect(dialog).toContainText('Renamed app');
	await page.keyboard.press('Escape');
	await expect(dialog).toHaveCount(0);
	await renamed.getByTestId('delete').click();
	await page.getByTestId('confirm-delete-dialog-yes').click();
	await expect(page.getByTestId('toast').filter({ hasText: 'Project deleted' })).toBeVisible();
	await expect(page.getByText('Renamed app')).toHaveCount(0);
	await page.reload();
	await expect(page.getByText('Renamed app')).toHaveCount(0);
});

test('the key is changed from the header on a later step and the action is retried', async ({
	page
}) => {
	await setPlanningKey(page, 'sk-first-key-0001');
	await page.goto('/#new');
	await expect(page.getByTestId('demo-badge')).toContainText('Demo mode');
	await expect(page.getByTestId('key-button')).toContainText('Claude (…0001)');

	// the key is removed in the header: the new project cannot write its questions and says why
	await page.getByTestId('key-button').click();
	await page.getByTestId('key-remove').click();
	await expect(page.getByTestId('key-button')).toContainText('No key');
	await page.getByTestId('key-close').click();
	await page.getByTestId('wish').fill('A key journey app');
	await page.getByTestId('create').click();
	await expect(page.getByTestId('no-planning-key')).toBeVisible();
	await expect(page.getByTestId('save-answers')).toBeDisabled();

	// the note offers the key panel; saving a new key starts the step by itself
	await page.getByTestId('open-key-panel').click();
	await page.getByTestId('key-input').fill('sk-second-key-0002');
	await page.getByTestId('key-save').click();
	await expect(page.getByTestId('key-button')).toContainText('Claude (…0002)');
	await page.getByTestId('key-close').click();
	await expect(page.getByTestId('question-1')).toBeVisible();

	// Test checks a key with the AI; the stub rejects "bad"; a failed check keeps the saved key
	await page.getByTestId('key-button').click();
	await page.getByTestId('key-input').fill('bad');
	await page.getByTestId('key-test').click();
	await expect(page.getByTestId('key-panel-result')).toContainText('Check failed');
	await expect(page.getByTestId('key-panel-result')).toContainText('saved earlier is unchanged');
	await expect(page.getByTestId('key-button')).toContainText('Claude (…0002)');
	await page.getByTestId('key-remove').click();
	await expect(page.getByTestId('key-button')).toContainText('No key');
	await page.keyboard.press('Escape');
	await expect(page.getByTestId('key-panel')).toHaveCount(0);
});

test('a Gemini key is saved with its preset, chosen for planning and removed', async ({ page }) => {
	await page.goto('/#projects');
	await page.getByTestId('key-button').click();
	await expect(page.getByTestId('key-preset')).toHaveValue('gemini');
	await expect(page.getByTestId('key-baseurl')).toHaveValue(
		'https://generativelanguage.googleapis.com/v1beta/openai/'
	);
	await expect(page.getByTestId('key-model')).toHaveValue('gemini-flash-lite-latest');

	await page.getByTestId('key-input-openai').fill('sk-ant-oops-0000');
	await expect(page.getByTestId('key-panel')).toContainText('Anthropic key');
	await page.getByTestId('key-input-openai').fill('AQ.gemini-secret-9999');
	await page.getByTestId('key-save-openai').click();
	await expect(page.getByTestId('key-button')).toContainText('Gemini (…9999)');
	await expect(page.getByTestId('key-planning')).toContainText('gemini-flash-lite-latest');
	await expect(page.getByTestId('key-panel')).not.toContainText('secret');

	await page.getByTestId('key-input').fill('sk-ant-other-key-7777');
	await page.getByTestId('key-save').click();
	await page.getByTestId('choose-openai').check();
	await expect(page.getByTestId('key-button')).toContainText('Gemini');
	await page.getByTestId('choose-anthropic').check();
	await expect(page.getByTestId('key-button')).toContainText('Claude (…7777)');

	await page.getByTestId('key-remove').click();
	await page.getByTestId('key-remove-openai').click();
	await expect(page.getByTestId('key-button')).toContainText('No key');
});
