import type { StepDef } from '#lib/api/client.js';

/** Moves the step at index by one place up (-1) or down (1); other indexes leave the list as it is. */
export function moveStep(steps: StepDef[], index: number, direction: -1 | 1): StepDef[] {
	const target = index + direction;
	if (index < 0 || index >= steps.length || target < 0 || target >= steps.length) return steps;
	const next = [...steps];
	[next[index], next[target]] = [next[target]!, next[index]!];
	return next;
}

/** A step is ready to save when title and prompt are filled and the cap is above 0 up to 10. */
export function stepProblem(step: StepDef): 'title' | 'prompt' | 'cap' | null {
	if (step.title.trim() === '' || step.title.trim().length > 80) return 'title';
	if (step.prompt.trim() === '' || step.prompt.trim().length > 4000) return 'prompt';
	if (!(step.capUsd > 0 && step.capUsd <= 10)) return 'cap';
	return null;
}

/** The profile that a project uses now, with the number of its steps, for the review table. */
export function profileSummary(profiles: { id: string; steps: unknown[] }[], id: string): number {
	return profiles.find((p) => p.id === id)?.steps.length ?? 0;
}
