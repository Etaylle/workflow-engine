import { expect, test } from '@playwright/test';
import { planToReview } from './helpers.js';

const SHOTS = process.env['SHOT_DIR'];

for (const [name, width, height] of [
	['wide', 2000, 1100],
	['laptop', 1280, 720]
] as const) {
	test(`the workflow diagram is visible on the review step, dark, ${name}`, async ({ page }) => {
		await page.emulateMedia({ colorScheme: 'dark' });
		await page.setViewportSize({ width, height });
		await planToReview(page, 'A canteen app where students order a meal and pick it up');

		const diagram = page.getByTestId('diagram');
		await expect(diagram.getByText('Order a meal')).toBeVisible();
		await expect(diagram.locator('.svelte-flow__node').first()).toBeVisible();
		await expect(diagram.locator('.svelte-flow__edge')).toHaveCount(5);
		const box = await diagram.boundingBox();
		expect(box?.height ?? 0).toBeGreaterThan(150);
		// every node lies inside the diagram frame after the fit
		for (const node of await diagram.locator('.svelte-flow__node').all()) {
			const nodeBox = await node.boundingBox();
			expect(nodeBox).not.toBeNull();
			expect(nodeBox!.x + nodeBox!.width).toBeLessThanOrEqual(box!.x + box!.width + 1);
			expect(nodeBox!.y + nodeBox!.height).toBeLessThanOrEqual(box!.y + box!.height + 1);
		}
		if (SHOTS) await page.screenshot({ path: `${SHOTS}/review-${name}.png`, fullPage: true });
	});
}

test('the wish form offers the builders and the planning line, dark', async ({ page }) => {
	await page.emulateMedia({ colorScheme: 'dark' });
	await page.setViewportSize({ width: 1280, height: 900 });
	await page.goto('/#new');
	await expect(page.getByTestId('planning-line')).toBeVisible();
	await expect(page.getByTestId('builder-claude_code')).toBeVisible();
	await expect(page.getByTestId('builder-gemini_cli')).toBeVisible();
	if (SHOTS) await page.screenshot({ path: `${SHOTS}/wish.png`, fullPage: true });
});
