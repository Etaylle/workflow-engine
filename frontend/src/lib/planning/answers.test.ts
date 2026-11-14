import { describe, expect, it } from 'vitest';
import type { Question } from '#lib/api/client.js';
import { asText, firstIncomplete, initialDraft, sameValue, toggle, toRequest } from './answers.js';

const question = (id: string, over: Partial<Question> = {}): Question => ({
	id,
	seq: 1,
	category: 'feature',
	answerType: 'single_choice',
	text: 'Q?',
	options: ['a', 'b'],
	proposal: 'a',
	...over
});

describe('initialDraft', () => {
	it('preselects the proposal and prefers a saved answer', () => {
		const saved = question('2', { answer: { value: 'b', decidedBy: 'client' } });
		expect(initialDraft([question('1'), saved])).toEqual({ '1': 'a', '2': 'b' });
	});
});

describe('toRequest', () => {
	it('sends untouched proposals as accepted and changed values as the clients own', () => {
		const multi = question('2', { answerType: 'multiple_choice', proposal: ['a', 'b'] });
		const request = toRequest([question('1'), multi], { '1': 'b', '2': ['b', 'a'] });
		expect(request).toEqual([
			{ questionId: '1', value: 'b' },
			{ questionId: '2', acceptProposal: true }
		]);
	});
});

describe('firstIncomplete', () => {
	it('finds an empty text or an empty selection', () => {
		const text = question('t', { answerType: 'text', proposal: 'x' });
		expect(firstIncomplete([text], { t: '  ' })?.id).toBe('t');
		const multi = question('m', { answerType: 'multiple_choice', proposal: ['a'] });
		expect(firstIncomplete([multi], { m: [] })?.id).toBe('m');
		expect(firstIncomplete([text, multi], { t: 'ok', m: ['a'] })).toBeNull();
	});
});

describe('values', () => {
	it('compares lists without order and toggles options', () => {
		expect(sameValue(['a', 'b'], ['b', 'a'])).toBe(true);
		expect(sameValue(['a'], 'a')).toBe(false);
		expect(toggle(['a'], 'b', true)).toEqual(['a', 'b']);
		expect(toggle(['a', 'b'], 'a', false)).toEqual(['b']);
		expect(asText(['a', 'b'])).toBe('a, b');
	});
});
