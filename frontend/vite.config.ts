import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';

export default defineConfig({
	plugins: [sveltekit({ adapter: adapter({ pages: 'build', assets: 'build', strict: true }) })],
	server: { proxy: { '/api': 'http://localhost:8080' } },
	test: { include: ['src/**/*.test.ts'], environment: 'node' }
});
