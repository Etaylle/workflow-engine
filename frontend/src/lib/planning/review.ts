import type { Question, Requirements, Workflow } from '#lib/api/client.js';

export type ReviewFacts = {
	answers: number;
	byAi: number;
	requirements: number;
	must: number;
	should: number;
	tasks: number;
	software: number;
	manual: number;
};

/** The counts shown on the review page, taken from what the project holds. */
export function reviewFacts(
	questions: Question[],
	requirements: Requirements,
	workflow: Workflow | null
): ReviewFacts {
	const all = requirements.epics.flatMap((epic) => epic.requirements);
	const tasks = workflow?.nodes.filter((node) => node.kind === 'task') ?? [];
	return {
		answers: questions.length,
		byAi: questions.filter((q) => q.answer?.decidedBy === 'ai').length,
		requirements: all.length,
		must: all.filter((r) => r.priority === 'must').length,
		should: all.filter((r) => r.priority === 'should').length,
		tasks: tasks.length,
		software: tasks.filter((node) => node.taskType === 'software').length,
		manual: tasks.filter((node) => node.taskType === 'manual').length
	};
}
