import type { Question, QuestionInput } from '#lib/api/client.js';

/** What the client types into the question editor; options and proposal are one entry per line. */
export type EditorState = {
	category: 'requirement' | 'feature';
	answerType: 'single_choice' | 'multiple_choice' | 'text';
	text: string;
	options: string;
	proposal: string;
};

export const emptyEditor = (): EditorState => ({
	category: 'feature',
	answerType: 'single_choice',
	text: '',
	options: '',
	proposal: ''
});

const lines = (value: string) =>
	value
		.split('\n')
		.map((line) => line.trim())
		.filter((line) => line !== '');

export function editorFrom(question: Question): EditorState {
	const proposal = Array.isArray(question.proposal) ? question.proposal : [question.proposal];
	return {
		category: question.category,
		answerType: question.answerType,
		text: question.text,
		options: question.options.join('\n'),
		proposal: proposal.join('\n')
	};
}

/** The request for the API: a text question has no options and exactly one proposal. */
export function toInput(state: EditorState): QuestionInput {
	const proposal = lines(state.proposal);
	return {
		category: state.category,
		answerType: state.answerType,
		text: state.text.trim(),
		options: state.answerType === 'text' ? [] : lines(state.options),
		proposal: state.answerType === 'multiple_choice' ? proposal : proposal.slice(0, 1)
	};
}
