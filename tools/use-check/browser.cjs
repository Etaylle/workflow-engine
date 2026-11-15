// Opens the page of a generated web app the way a client would and reports what does not work.
// Usage: node browser.cjs <url> <screenshot.png>   (NODE_PATH must lead to playwright)
// Output: lines "FINDING: ..." for what is broken and "INFO: ..." for what was seen.
// Exit code: 0 nothing found, 1 findings, 2 no browser tooling installed.
'use strict';

const MAIN_CONTROL = /start|play|begin|new game|restart|spiel|los|neu/i;
const KEYS = ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Space', 'Enter'];
const MAX_FINDINGS = 12;

function loadChromium() {
	for (const name of ['playwright', '@playwright/test', 'playwright-core']) {
		try {
			return require(name).chromium;
		} catch {
			// try the next package
		}
	}
	return null;
}

function watch(page, origin, findings) {
	page.on('pageerror', (error) => findings.add(`script error: ${error.message}`));
	page.on('console', (message) => {
		if (message.type() === 'error' && !/favicon/i.test(message.text())) {
			findings.add(`console error: ${message.text()}`);
		}
	});
	page.on('response', (response) => {
		const url = response.url();
		if (response.status() >= 400 && url.startsWith(origin) && !/favicon/i.test(url)) {
			const path = url.slice(origin.length) || '/';
			findings.add(`request failed: ${response.request().method()} ${path} answered ${response.status()}`);
		}
	});
}

async function pressMainControls(page) {
	const button = page.getByRole('button', { name: MAIN_CONTROL }).first();
	let pressed = '';
	if ((await button.count()) > 0 && (await button.isVisible())) {
		pressed = ((await button.innerText()) || '').trim();
		await button.click({ timeout: 3000 }).catch(() => {});
	}
	await page.mouse.click(640, 400).catch(() => {});
	for (const key of KEYS) {
		await page.keyboard.press(key).catch(() => {});
	}
	return pressed;
}

function facts() {
	const blank = (canvas) => {
		const empty = document.createElement('canvas');
		empty.width = canvas.width;
		empty.height = canvas.height;
		try {
			return canvas.toDataURL() === empty.toDataURL();
		} catch {
			return false;
		}
	};
	const canvases = [...document.querySelectorAll('canvas')];
	return {
		title: document.title,
		text: (document.body ? document.body.innerText : '').trim().replace(/\s+/g, ' '),
		canvases: canvases.length,
		blankCanvases: canvases.filter(blank).length,
		pictures: document.querySelectorAll('img, svg, video').length
	};
}

async function use(chromium, url, screenshot) {
	const findings = new Set();
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
		watch(page, new URL(url).origin, findings);
		const response = await page.goto(url, { waitUntil: 'load', timeout: 30000 });
		if (!response || response.status() >= 400) {
			findings.add(`the page answered ${response ? response.status() : 'nothing'}`);
		}
		await page.waitForTimeout(1500);
		const pressed = await pressMainControls(page);
		await page.waitForTimeout(2000);
		const seen = await page.evaluate(facts);
		if (!seen.text && seen.canvases === 0 && seen.pictures === 0) {
			findings.add('the page shows nothing: no text, no canvas, no picture');
		}
		if (seen.canvases > 0 && seen.blankCanvases === seen.canvases) {
			findings.add('the canvas is still empty after the start control and the keys were pressed');
		}
		await page.screenshot({ path: screenshot }).catch(() => {});
		console.log(`INFO: title "${seen.title}", ${seen.canvases} canvas, text: ${seen.text.slice(0, 160)}`);
		console.log(`INFO: pressed ${pressed ? `the button "${pressed}", ` : ''}a click and the keys ${KEYS.join(', ')}`);
	} finally {
		await browser.close();
	}
	return [...findings].slice(0, MAX_FINDINGS);
}

async function main() {
	const [url, screenshot] = process.argv.slice(2);
	if (!url || !screenshot) {
		console.log('usage: node browser.cjs <url> <screenshot.png>');
		process.exit(1);
	}
	const chromium = loadChromium();
	if (!chromium) {
		console.log('INFO: no browser tooling (playwright) found');
		process.exit(2);
	}
	const findings = await use(chromium, url, screenshot);
	for (const finding of findings) {
		console.log(`FINDING: ${finding}`);
	}
	process.exit(findings.length > 0 ? 1 : 0);
}

main().catch((error) => {
	console.log(`FINDING: the browser could not open the page: ${error.message}`);
	process.exit(1);
});
