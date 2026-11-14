import { svelte } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vitest/config';

export default defineConfig({
	plugins: [svelte()],
	server: { proxy: { '/api': 'http://localhost:18080' } },
	test: { include: ['src/**/*.test.ts'], environment: 'node' }
});
