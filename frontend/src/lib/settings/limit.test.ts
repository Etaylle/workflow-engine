import { describe, expect, it } from 'vitest';
import { parseLimit } from './limit.js';

describe('parseLimit', () => {
	it('takes an empty field as the default', () => {
		expect(parseLimit('')).toBeNull();
		expect(parseLimit('  ')).toBeNull();
	});
	it('reads whole and decimal amounts, comma or point', () => {
		expect(parseLimit('15')).toBe(15);
		expect(parseLimit('12.5')).toBe(12.5);
		expect(parseLimit('12,50')).toBe(12.5);
	});
	it('refuses text, negatives, too many decimals and the range ends', () => {
		for (const bad of ['abc', '-5', '1.234', '0.5', '100001', '1e3'])
			expect(parseLimit(bad)).toBe('invalid');
		expect(parseLimit('1')).toBe(1);
		expect(parseLimit('100000')).toBe(100000);
	});
});
