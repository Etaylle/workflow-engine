import { isStepId, type StepId } from '#lib/planning/steps.js';

/**
 * The screen the address hash points to: the list (empty or #projects), the new-project form,
 * one step of a project (#<id>/questions; #<id> alone means the step to work on), or the team
 * and steps page of a project (#<id>/steps).
 */
export type Route =
	| { name: 'list' }
	| { name: 'new' }
	| { name: 'project'; id: string; step: StepId | null }
	| { name: 'steps'; id: string };

export function parseRoute(hash: string): Route {
	const value = hash.replace(/^#/, '');
	if (value === '' || value === 'projects') return { name: 'list' };
	if (value === 'new') return { name: 'new' };
	const slash = value.indexOf('/');
	if (slash < 0) return { name: 'project', id: value, step: null };
	const id = value.slice(0, slash);
	const rest = value.slice(slash + 1);
	if (rest === 'steps') return { name: 'steps', id };
	return { name: 'project', id, step: isStepId(rest) ? rest : null };
}

/** The address of one step of a project. */
export function stepHash(id: string, step: StepId): string {
	return `#${id}/${step}`;
}
