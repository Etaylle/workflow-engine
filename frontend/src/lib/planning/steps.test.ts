import { describe, expect, it } from 'vitest';
import {
	clampStep,
	nextActionKey,
	defaultStep,
	nextStep,
	previousStep,
	rank,
	reach
} from './steps.js';

describe('journey position', () => {
	it('orders the statuses', () => {
		expect(rank('draft')).toBeLessThan(rank('answered'));
		expect(rank('requirements_ready')).toBeLessThan(rank('workflow_drafted'));
		expect(rank('executing')).toBe(rank('confirmed'));
	});

	it('names the furthest step the project may open', () => {
		expect(reach('draft')).toBe(1);
		expect(reach('questions_ready')).toBe(1);
		expect(reach('answered')).toBe(2);
		expect(reach('requirements_ready')).toBe(3);
		expect(reach('workflow_drafted')).toBe(4);
		expect(reach('confirmed')).toBe(5);
		expect(reach('executing')).toBe(5);
	});

	it('opens the step the client works on next', () => {
		expect(defaultStep('draft')).toBe('questions');
		expect(defaultStep('answered')).toBe('requirements');
		expect(defaultStep('workflow_drafted')).toBe('review');
		expect(defaultStep('confirmed')).toBe('run');
	});

	it('keeps a finished step and refuses a step that is not reached yet', () => {
		expect(clampStep('wish', 'workflow_drafted')).toBe('wish');
		expect(clampStep('questions', 'workflow_drafted')).toBe('questions');
		expect(clampStep('run', 'answered')).toBe('requirements');
		expect(clampStep(null, 'answered')).toBe('requirements');
	});

	it('walks back and forth', () => {
		expect(previousStep('wish')).toBeNull();
		expect(previousStep('review')).toBe('workflow');
		expect(nextStep('review')).toBe('run');
		expect(nextStep('run')).toBeNull();
	});

	it('names the next required action only before the run', () => {
		expect(nextActionKey('answered')).toBe('next.answered');
		expect(nextActionKey('executing')).toBeNull();
	});
});
