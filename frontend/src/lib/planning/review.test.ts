import { describe, expect, it } from 'vitest';
import type { Question, Requirements, Workflow } from '#lib/api/client.js';
import { reviewFacts } from './review.js';

const question = (decidedBy: 'ai' | 'client' | null): Question => ({
	id: 'q',
	seq: 1,
	category: 'feature',
	answerType: 'text',
	text: 't',
	options: [],
	proposal: 'p',
	answer: decidedBy ? { value: 'v', decidedBy } : null
});

describe('reviewFacts', () => {
	it('counts answers, priorities and task types', () => {
		const requirements: Requirements = {
			epics: [
				{
					key: 'E-01',
					name: 'n',
					domainName: 'd',
					requirements: [
						{ key: 'FR-001', type: 'functional', priority: 'must', text: 'a' },
						{ key: 'FR-002', type: 'functional', priority: 'should', text: 'b' }
					]
				}
			]
		};
		const workflow = {
			nodes: [
				{ kind: 'start_event' },
				{ kind: 'task', taskType: 'software' },
				{ kind: 'task', taskType: 'manual' }
			]
		} as Workflow;
		expect(reviewFacts([question('ai'), question('client')], requirements, workflow)).toEqual({
			answers: 2,
			byAi: 1,
			requirements: 2,
			must: 1,
			should: 1,
			tasks: 2,
			software: 1,
			manual: 1
		});
	});
	it('is all zero for an empty project', () => {
		expect(reviewFacts([], { epics: [] }, null).tasks).toBe(0);
	});
});
