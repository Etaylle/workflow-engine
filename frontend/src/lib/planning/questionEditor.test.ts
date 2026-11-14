import { describe, expect, it } from 'vitest';
import type { Question } from '#lib/api/client.js';
import { editorFrom, emptyEditor, toInput } from './questionEditor.js';

describe('question editor', () => {
	it('turns lines into options and proposal, dropping blanks', () => {
		const input = toInput({
			...emptyEditor(),
			text: ' Pay how? ',
			options: 'Cash\n\n Card \n',
			proposal: 'Card\nCash'
		});
		expect(input).toEqual({
			category: 'feature',
			answerType: 'single_choice',
			text: 'Pay how?',
			options: ['Cash', 'Card'],
			proposal: ['Card']
		});
	});

	it('keeps every proposal line for a multiple choice', () => {
		const input = toInput({
			...emptyEditor(),
			answerType: 'multiple_choice',
			options: 'a\nb',
			proposal: 'a\nb'
		});
		expect(input.proposal).toEqual(['a', 'b']);
	});

	it('gives a text question no options', () => {
		const input = toInput({ ...emptyEditor(), answerType: 'text', options: 'x', proposal: 'free' });
		expect(input.options).toEqual([]);
		expect(input.proposal).toEqual(['free']);
	});

	it('fills the editor from a question', () => {
		const question: Question = {
			id: '1',
			seq: 1,
			category: 'requirement',
			answerType: 'multiple_choice',
			text: 'Which?',
			options: ['a', 'b'],
			proposal: ['a']
		};
		expect(editorFrom(question)).toMatchObject({ options: 'a\nb', proposal: 'a' });
	});
});
