import { describe, expect, it } from 'vitest';
import { currentStep, rank } from './steps.js';

describe('journey position', () => {
	it('orders the statuses', () => {
		expect(rank('draft')).toBeLessThan(rank('answered'));
		expect(rank('requirements_ready')).toBeLessThan(rank('workflow_drafted'));
		expect(rank('executing')).toBe(rank('confirmed'));
	});

	it('names the step to work on', () => {
		expect(currentStep('draft')).toBe('questions');
		expect(currentStep('questions_ready')).toBe('questions');
		expect(currentStep('answered')).toBe('requirements');
		expect(currentStep('requirements_ready')).toBe('workflow');
		expect(currentStep('workflow_drafted')).toBe('review');
		expect(currentStep('confirmed')).toBe('review');
	});
});
