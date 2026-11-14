/** Project statuses in the order of the journey; the screens show every step up to the status. */
const ORDER = [
	'draft',
	'questions_ready',
	'answered',
	'requirements_ready',
	'workflow_drafted',
	'confirmed'
] as const;

/** Position of a status on the journey (0 for a status beyond the planning). */
export function rank(status: string): number {
	const index = ORDER.indexOf(status as (typeof ORDER)[number]);
	return index < 0 ? ORDER.length - 1 : index;
}

/** Index in the header chips (wish, questions, requirements, workflow, review) of the step to work on. */
export function activeChip(status: string): number {
	const r = rank(status);
	if (r <= 1) return 1;
	if (r === 2) return 2;
	if (r === 3) return 3;
	return 4;
}
