import { defineConfig } from '@playwright/test';

// The engine (with ENGINE_AI_STUB=true) is started by `ci/run test-e2e`.
const baseURL = process.env['ENGINE_E2E_URL'] ?? 'http://localhost:8081';
// The tests create projects: never against the engine of the owner (port 8080, database engine).
if (new URL(baseURL).port === '8080') {
	throw new Error(
		`Refusing to run the end-to-end tests against ${baseURL}: it is the main engine. Use ci/run test-e2e.`
	);
}

export default defineConfig({
	testDir: 'e2e',
	reporter: 'list',
	// One engine holds one session key: tests must not run in parallel.
	workers: 1,
	use: { baseURL }
});
