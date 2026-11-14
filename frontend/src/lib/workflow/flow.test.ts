import { describe, expect, it } from 'vitest';
import type { Workflow } from '#lib/api/client.js';
import { toFlow } from './flow.js';

const workflow: Workflow = {
	id: 'w',
	projectId: 'p',
	status: 'valid',
	version: 0,
	lanes: [
		{ id: 'l1', name: 'Student' },
		{ id: 'l2', name: 'Kitchen' }
	],
	nodes: [
		{ id: 'a', laneId: 'l1', kind: 'start_event', title: 'Hungry', posX: 0, posY: 0 },
		{ id: 'b', laneId: 'l2', kind: 'task', title: 'Cook', taskType: 'manual', posX: 260, posY: 180 }
	],
	edges: [{ id: 'e', source: 'a', target: 'b' }]
};

describe('toFlow', () => {
	const flow = toFlow(workflow, { taskType: (type) => `T:${type}` });

	it('keeps the engine positions and adds one label per lane', () => {
		expect(flow.nodes.find((n) => n.id === 'b')?.position).toEqual({ x: 260, y: 180 });
		expect(flow.nodes.filter((n) => n.class === 'lane-label')).toHaveLength(2);
	});

	it('shows the task type next to the title and maps kinds to node types', () => {
		expect(flow.nodes.find((n) => n.id === 'b')?.data['label']).toBe('Cook (T:manual)');
		expect(flow.nodes.find((n) => n.id === 'a')?.type).toBe('input');
	});

	it('copies the edges', () => {
		expect(flow.edges).toHaveLength(1);
		expect(flow.edges[0]).toMatchObject({ source: 'a', target: 'b' });
	});

	it('handles a workflow without edges', () => {
		expect(toFlow({ ...workflow, edges: [] }, { taskType: String }).edges).toEqual([]);
	});
});
