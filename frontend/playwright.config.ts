import { defineConfig } from '@playwright/test';

// The engine (with ENGINE_AI_STUB=true) is started by `ci/run test-e2e`.
export default defineConfig({
	testDir: 'e2e',
	reporter: 'list',
	use: { baseURL: process.env['ENGINE_E2E_URL'] ?? 'http://localhost:8081' }
});
