import { describe, expect, it } from 'vitest';
import type { StepDef } from '#lib/api/client.js';
import { moveStep, profileSummary, stepProblem } from './steps.js';

const step = (key: string): StepDef => ({
	key,
	title: key,
	gated: false,
	review: false,
	capUsd: 1,
	prompt: 'p'
});

describe('moveStep', () => {
	const steps = [step('a'), step('b'), step('c')];
	it('swaps with the neighbour', () => {
		expect(moveStep(steps, 1, -1).map((s) => s.key)).toEqual(['b', 'a', 'c']);
		expect(moveStep(steps, 1, 1).map((s) => s.key)).toEqual(['a', 'c', 'b']);
	});
	it('keeps the list at the ends and for a bad index', () => {
		expect(moveStep(steps, 0, -1)).toBe(steps);
		expect(moveStep(steps, 2, 1)).toBe(steps);
		expect(moveStep(steps, 7, -1)).toBe(steps);
	});
});

describe('stepProblem', () => {
	it('accepts a complete step', () => expect(stepProblem(step('a'))).toBeNull());
	it('names the first broken field', () => {
		expect(stepProblem({ ...step('a'), title: ' ' })).toBe('title');
		expect(stepProblem({ ...step('a'), prompt: '' })).toBe('prompt');
		expect(stepProblem({ ...step('a'), capUsd: 0 })).toBe('cap');
		expect(stepProblem({ ...step('a'), capUsd: 10.5 })).toBe('cap');
		expect(stepProblem({ ...step('a'), capUsd: Number.NaN })).toBe('cap');
	});
});

describe('profileSummary', () => {
	it('counts the steps of the profile, 0 when unknown', () => {
		expect(profileSummary([{ id: 'x', steps: [1, 2] }], 'x')).toBe(2);
		expect(profileSummary([], 'x')).toBe(0);
	});
});
