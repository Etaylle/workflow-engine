import { expect, test } from '@playwright/test';

test('wish to workflow diagram to confirmed contract, and back after a reload', async ({
	page
}) => {
	await page.goto('/');

	await page.getByTestId('wish').fill('A canteen app where students order a meal and pick it up');
	await page.getByTestId('project-key').fill('sk-any');
	await page.getByRole('button', { name: 'Create project' }).click();

	await page.getByTestId('generate').click();
	const diagram = page.getByTestId('diagram');
	await expect(diagram.getByText('Order a meal')).toBeVisible();
	await expect(diagram.getByText('Prepare the meal')).toBeVisible();
	await expect(diagram.locator('.svelte-flow__edge')).toHaveCount(5);

	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');

	await page.reload();
	await expect(page.getByTestId('projects')).toContainText('A canteen app');
	await expect(page.getByTestId('diagram').getByText('Order a meal')).toBeVisible();
	await expect(page.getByTestId('confirmed')).toContainText('contract version 1');

	await page.getByTestId('generate').click();
	await page.getByTestId('confirm').click();
	await expect(page.getByTestId('versions')).toContainText('1, 2');
});
