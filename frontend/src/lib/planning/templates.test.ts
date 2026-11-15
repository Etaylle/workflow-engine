import { describe, expect, it } from 'vitest';
import type { Template } from '#lib/api/client.js';
import { templateOptions } from './templates.js';

const t = (id: string, kind: 'console' | 'web' = 'web'): Template => ({
	id,
	name: id,
	kind,
	description: ''
});

describe('templateOptions', () => {
	it('puts the console program first and the heavy web app last', () => {
		const ids = templateOptions([
			t('java-svelte-postgres'),
			t('sveltekit-sqlite'),
			t('node-console', 'console')
		]).map((x) => x.id);

		expect(ids).toEqual(['node-console', 'sveltekit-sqlite', 'java-svelte-postgres']);
	});

	it('keeps an unknown template after the known ones, sorted by id', () => {
		const ids = templateOptions([t('zeta'), t('alpha'), t('sveltekit-sqlite')]).map((x) => x.id);

		expect(ids).toEqual(['sveltekit-sqlite', 'alpha', 'zeta']);
	});

	it('returns an empty list for no templates', () => {
		expect(templateOptions([])).toEqual([]);
	});
});
