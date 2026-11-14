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

export const STEPS = ['questions', 'requirements', 'workflow', 'review'] as const;
export type Step = (typeof STEPS)[number];

/** The step the client works on at the status: done steps are behind it. */
export function currentStep(status: string): Step {
	const r = rank(status);
	if (r <= 1) return 'questions';
	if (r === 2) return 'requirements';
	if (r === 3) return 'workflow';
	return 'review';
}
