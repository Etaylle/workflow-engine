import { describe, expect, it } from 'vitest';
import { activeChip, rank } from './steps.js';

describe('journey position', () => {
	it('orders the statuses', () => {
		expect(rank('draft')).toBeLessThan(rank('answered'));
		expect(rank('requirements_ready')).toBeLessThan(rank('workflow_drafted'));
		expect(rank('executing')).toBe(rank('confirmed'));
	});

	it('names the header chip to work on', () => {
		expect(activeChip('draft')).toBe(1);
		expect(activeChip('questions_ready')).toBe(1);
		expect(activeChip('answered')).toBe(2);
		expect(activeChip('requirements_ready')).toBe(3);
		expect(activeChip('workflow_drafted')).toBe(4);
		expect(activeChip('confirmed')).toBe(4);
	});
});
