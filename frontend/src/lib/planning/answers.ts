import type { AnswerInput, AnswerValue, Question } from '#lib/api/client.js';

/** What the client sees for one question: the current value, starting at the AI's proposal. */
export type Draft = Record<string, AnswerValue>;

/** The saved answer of each question, else its proposal (D-23: the proposal is preselected). */
export function initialDraft(questions: Question[]): Draft {
	const draft: Draft = {};
	for (const question of questions) {
		draft[question.id] = question.answer ? question.answer.value : question.proposal;
	}
	return draft;
}

/** Order-independent equality of two answer values. */
export function sameValue(a: AnswerValue, b: AnswerValue): boolean {
	if (Array.isArray(a) && Array.isArray(b)) {
		return a.length === b.length && [...a].sort().every((item, i) => item === [...b].sort()[i]);
	}
	return a === b;
}

/** A value equal to the proposal is sent as "accept proposal"; anything else as the client's. */
export function toRequest(questions: Question[], draft: Draft): AnswerInput[] {
	return questions.map((question) => {
		const value = draft[question.id] ?? question.proposal;
		return sameValue(value, question.proposal)
			? { questionId: question.id, acceptProposal: true }
			: { questionId: question.id, value };
	});
}

/** The first question whose draft cannot be saved (empty text or no option), else null. */
export function firstIncomplete(questions: Question[], draft: Draft): Question | null {
	for (const question of questions) {
		const value = draft[question.id];
		const empty =
			value === undefined || (Array.isArray(value) ? value.length === 0 : value.trim() === '');
		if (empty) return question;
	}
	return null;
}

/** Adds or removes an option of a multiple-choice value. */
export function toggle(value: AnswerValue, option: string, on: boolean): string[] {
	const items = Array.isArray(value) ? value : [value];
	return on
		? [...items.filter((item) => item !== option), option]
		: items.filter((item) => item !== option);
}

/** Shows an answer value as one line. */
export function asText(value: AnswerValue): string {
	return Array.isArray(value) ? value.join(', ') : value;
}
