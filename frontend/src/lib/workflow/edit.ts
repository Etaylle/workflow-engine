import type { Workflow } from '#lib/api/client.js';

export type DraftNode = {
	key: string;
	lane: string;
	kind: 'start_event' | 'task' | 'end_event';
	title: string;
	description: string;
	taskType: 'software' | 'manual' | null;
};
export type DraftEdge = { from: string; to: string };
/** The workflow the client edits; the node key only links the edges. */
export type Draft = { lanes: string[]; nodes: DraftNode[]; edges: DraftEdge[] };

export function fromWorkflow(workflow: Workflow): Draft {
	const laneName = new Map(workflow.lanes.map((lane) => [lane.id, lane.name]));
	return {
		lanes: workflow.lanes.map((lane) => lane.name),
		nodes: workflow.nodes.map((node) => ({
			key: node.id,
			lane: laneName.get(node.laneId) ?? '',
			kind: node.kind,
			title: node.title,
			description: node.description ?? '',
			taskType: node.taskType ?? null
		})),
		edges: workflow.edges.map((edge) => ({ from: edge.source, to: edge.target }))
	};
}

const hasEdge = (edges: DraftEdge[], from: string, to: string) =>
	edges.some((edge) => edge.from === from && edge.to === to);

/** A new software task behind the node; the arrows that left the node now leave the new task. */
export function addAfter(draft: Draft, key: string, newKey: string, title: string): Draft {
	const anchor = draft.nodes.find((node) => node.key === key);
	if (!anchor || anchor.kind === 'end_event') return draft;
	const node: DraftNode = {
		key: newKey,
		lane: anchor.lane,
		kind: 'task',
		title,
		description: '',
		taskType: 'software'
	};
	const moved = draft.edges.filter((edge) => edge.from === key);
	return {
		...draft,
		nodes: [...draft.nodes, node],
		edges: [
			...draft.edges.filter((edge) => edge.from !== key),
			{ from: key, to: newKey },
			...moved.map((edge) => ({ from: newKey, to: edge.to }))
		]
	};
}

/** Removes a node and joins each arrow into it with each arrow out of it. The start stays. */
export function removeNode(draft: Draft, key: string): Draft {
	const node = draft.nodes.find((candidate) => candidate.key === key);
	if (!node || node.kind === 'start_event') return draft;
	const into = draft.edges.filter((edge) => edge.to === key);
	const out = draft.edges.filter((edge) => edge.from === key);
	const kept = draft.edges.filter((edge) => edge.from !== key && edge.to !== key);
	for (const a of into) {
		for (const b of out) {
			if (a.from !== b.to && !hasEdge(kept, a.from, b.to)) kept.push({ from: a.from, to: b.to });
		}
	}
	return { ...draft, nodes: draft.nodes.filter((candidate) => candidate.key !== key), edges: kept };
}

export function addEdge(draft: Draft, from: string, to: string): Draft {
	if (from === to || hasEdge(draft.edges, from, to)) return draft;
	return { ...draft, edges: [...draft.edges, { from, to }] };
}

export function removeEdge(draft: Draft, from: string, to: string): Draft {
	return { ...draft, edges: draft.edges.filter((e) => !(e.from === from && e.to === to)) };
}

/** Adds a lane unless the name is blank or taken. */
export function addLane(draft: Draft, name: string): Draft {
	const trimmed = name.trim();
	if (trimmed === '' || draft.lanes.includes(trimmed)) return draft;
	return { ...draft, lanes: [...draft.lanes, trimmed] };
}

/** The request body: titles trimmed, a description only when there is one, no type for events. */
export function toRequest(draft: Draft): unknown {
	return {
		lanes: draft.lanes,
		nodes: draft.nodes.map((node) => ({
			key: node.key,
			lane: node.lane,
			kind: node.kind,
			title: node.title.trim(),
			description: node.description.trim() === '' ? null : node.description.trim(),
			taskType: node.kind === 'task' ? node.taskType : null
		})),
		edges: draft.edges
	};
}
