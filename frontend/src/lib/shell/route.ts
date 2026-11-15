/** The screen the address hash points to: the list (empty or #projects), the new-project form, one project, or the steps of a project (#<id>/steps). */
export type Route =
	| { name: 'list' }
	| { name: 'new' }
	| { name: 'project'; id: string }
	| { name: 'steps'; id: string };

export function parseRoute(hash: string): Route {
	const value = hash.replace(/^#/, '');
	if (value === '' || value === 'projects') return { name: 'list' };
	if (value === 'new') return { name: 'new' };
	if (value.endsWith('/steps')) return { name: 'steps', id: value.slice(0, -'/steps'.length) };
	return { name: 'project', id: value };
}
