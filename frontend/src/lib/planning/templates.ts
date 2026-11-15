import type { Template } from '#lib/api/client.js';

/** The order of the choices on the review step: the free choice first, then the prepared templates from light to heavy. */
const ORDER = ['free', 'node-console', 'sveltekit-sqlite', 'java-svelte-postgres'];

/** The templates the engine offers, in the order of the review step; unknown ids come last. */
export function templateOptions(templates: Template[]): Template[] {
	const rank = (id: string) => {
		const at = ORDER.indexOf(id);
		return at === -1 ? ORDER.length : at;
	};
	return [...templates].sort((a, b) => rank(a.id) - rank(b.id) || a.id.localeCompare(b.id));
}
