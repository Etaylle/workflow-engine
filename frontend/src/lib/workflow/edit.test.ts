import { describe, expect, it } from 'vitest';
import {
	addAfter,
	addEdge,
	addLane,
	removeEdge,
	removeNode,
	toRequest,
	type Draft
} from './edit.js';

const node = (key: string, kind: 'start_event' | 'task' | 'end_event', lane = 'A') => ({
	key,
	lane,
	kind,
	title: key,
	description: '',
	taskType: kind === 'task' ? ('software' as const) : null
});

const base = (): Draft => ({
	lanes: ['A', 'B'],
	nodes: [node('s', 'start_event'), node('t', 'task', 'B'), node('e', 'end_event')],
	edges: [
		{ from: 's', to: 't' },
		{ from: 't', to: 'e' }
	]
});

describe('addAfter', () => {
	it('puts a task in the lane of the node and reroutes its arrows', () => {
		const draft = addAfter(base(), 't', 'n', 'New');
		expect(draft.nodes.find((n) => n.key === 'n')).toMatchObject({ lane: 'B', kind: 'task' });
		expect(draft.edges).toEqual([
			{ from: 's', to: 't' },
			{ from: 't', to: 'n' },
			{ from: 'n', to: 'e' }
		]);
	});
	it('does nothing behind an end event or an unknown node', () => {
		expect(addAfter(base(), 'e', 'n', 'New')).toEqual(base());
		expect(addAfter(base(), 'zzz', 'n', 'New')).toEqual(base());
	});
});

describe('removeNode', () => {
	it('reconnects what led into the node with what left it', () => {
		const draft = removeNode(base(), 't');
		expect(draft.nodes.map((n) => n.key)).toEqual(['s', 'e']);
		expect(draft.edges).toEqual([{ from: 's', to: 'e' }]);
	});
	it('keeps the start event', () => {
		expect(removeNode(base(), 's')).toEqual(base());
	});
});

describe('edges and lanes', () => {
	it('ignores loops and duplicates, removes an arrow', () => {
		expect(addEdge(base(), 's', 's')).toEqual(base());
		expect(addEdge(base(), 's', 't')).toEqual(base());
		expect(addEdge(base(), 's', 'e').edges).toHaveLength(3);
		expect(removeEdge(base(), 's', 't').edges).toEqual([{ from: 't', to: 'e' }]);
	});
	it('adds a lane once and never a blank one', () => {
		expect(addLane(base(), ' C ').lanes).toEqual(['A', 'B', 'C']);
		expect(addLane(base(), 'A').lanes).toEqual(['A', 'B']);
		expect(addLane(base(), '  ').lanes).toEqual(['A', 'B']);
	});
});

describe('toRequest', () => {
	it('trims, nulls an empty description and gives events no type', () => {
		const draft = base();
		draft.nodes[1] = { ...node('t', 'task'), title: ' Pay ', description: '  ' };
		draft.nodes[0] = { ...node('s', 'start_event'), taskType: 'software' };
		const body = toRequest(draft) as {
			nodes: { title: string; description: unknown; taskType: unknown }[];
		};
		expect(body.nodes[1]).toMatchObject({ title: 'Pay', description: null, taskType: 'software' });
		expect(body.nodes[0]?.taskType).toBeNull();
	});
});
