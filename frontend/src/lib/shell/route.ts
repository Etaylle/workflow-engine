/** The screen the address hash points to: the list (empty or #projects), the new-project form or one project. */
export type Route = { name: 'list' } | { name: 'new' } | { name: 'project'; id: string };

export function parseRoute(hash: string): Route {
	const value = hash.replace(/^#/, '');
	if (value === '' || value === 'projects') return { name: 'list' };
	if (value === 'new') return { name: 'new' };
	return { name: 'project', id: value };
}
