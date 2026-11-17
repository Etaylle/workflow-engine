import { describe, expect, it } from 'vitest';
import type { StepDef } from '#lib/api/client.js';
import { moveStep, profileSummary, stepProblem } from './steps.js';

const step = (key: string): StepDef => ({
	key,
	title: key,
	gated: false,
	review: false,
	reviewerOnly: false,
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
		expect(stepProblem({ ...step('a'), capUsd: 1000.5 })).toBe('cap');
		expect(stepProblem({ ...step('a'), capUsd: Number.NaN })).toBe('cap');
	});
	it('accepts a step without a cap, and a cap up to 1000', () => {
		expect(stepProblem({ ...step('a'), capUsd: null })).toBeNull();
		expect(stepProblem({ ...step('a'), capUsd: 1000 })).toBeNull();
	});
});

describe('profileSummary', () => {
	const profiles = [{ id: 'x', steps: [step('plan'), { ...step('opinion'), reviewerOnly: true }] }];
	it('counts the steps of the profile, 0 when unknown', () => {
		expect(profileSummary(profiles, 'x', true)).toBe(2);
		expect(profileSummary([], 'x', true)).toBe(0);
	});
	it('leaves out the steps that need a reviewer when the project has none', () => {
		expect(profileSummary(profiles, 'x', false)).toBe(1);
	});
});
