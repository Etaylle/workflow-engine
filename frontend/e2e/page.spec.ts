import { expect, test } from '@playwright/test';

test('status, language switch and AI check with the stub', async ({ page }) => {
	await page.goto('/');
	await expect(page.getByTestId('status')).toContainText('workflow-engine');
	await expect(page.getByRole('heading', { name: 'Engine status' })).toBeVisible();

	await page.getByTestId('language').selectOption('de');
	await expect(page.getByRole('heading', { name: 'Status der Engine' })).toBeVisible();
	await page.getByTestId('language').selectOption('en');

	await page.getByLabel('Anthropic key', { exact: true }).fill('bad');
	await page.getByRole('button', { name: 'Check connection' }).click();
	await expect(page.getByTestId('ai-result')).toContainText('Check failed');

	await page.getByLabel('Anthropic key', { exact: true }).fill('sk-any');
	await page.getByRole('button', { name: 'Check connection' }).click();
	await expect(page.getByTestId('ai-result')).toContainText('Connected');
});
