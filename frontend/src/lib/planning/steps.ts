/** Project statuses in the order of the journey; the screens show every step up to the status. */
const ORDER = [
	'draft',
	'questions_ready',
	'answered',
	'requirements_ready',
	'workflow_drafted',
	'confirmed'
] as const;

/** The views of a project, one per step, in journey order. */
export const STEPS = ['wish', 'questions', 'requirements', 'workflow', 'review', 'run'] as const;

export type StepId = (typeof STEPS)[number];

export function isStepId(value: string): value is StepId {
	return (STEPS as readonly string[]).includes(value);
}

/** Position of a status on the journey (0 for a status beyond the planning). */
export function rank(status: string): number {
	const index = ORDER.indexOf(status as (typeof ORDER)[number]);
	return index < 0 ? ORDER.length - 1 : index;
}

/** Index in STEPS of the furthest step the project may open: the step the client works on next. */
export function reach(status: string): number {
	const r = rank(status);
	return r <= 1 ? 1 : r;
}

/** The step to open for a project when the address names none. */
export function defaultStep(status: string): StepId {
	return STEPS[reach(status)] ?? 'wish';
}

/** The step to show for a requested step: never further than the project has reached. */
export function clampStep(requested: StepId | null, status: string): StepId {
	if (requested === null) return defaultStep(status);
	return STEPS.indexOf(requested) > reach(status) ? defaultStep(status) : requested;
}

/** The step before this one; the wish step goes back to the list (null). */
export function previousStep(step: StepId): StepId | null {
	const index = STEPS.indexOf(step);
	return index <= 0 ? null : (STEPS[index - 1] ?? null);
}

/** The step after this one (null after the last). */
export function nextStep(step: StepId): StepId | null {
	return STEPS[STEPS.indexOf(step) + 1] ?? null;
}

/** Message key naming the next required action of a status, or null once the run is under way. */
export function nextActionKey(status: string): string | null {
	return (ORDER as readonly string[]).includes(status) ? `next.${status}` : null;
}
