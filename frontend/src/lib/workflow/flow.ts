import type { Edge, MarkerType, Node } from '@xyflow/svelte';
import type { Workflow } from '#lib/api/client.js';

const LANE_LABEL_X = -200;
const LANE_HEIGHT = 180;

export type FlowLabels = { taskType: (type: 'software' | 'manual') => string };

/** Turns the API workflow into Svelte Flow nodes and edges (positions come from the engine). */
export function toFlow(workflow: Workflow, labels: FlowLabels): { nodes: Node[]; edges: Edge[] } {
	const laneNodes: Node[] = workflow.lanes.map((lane, index) => ({
		id: `lane-${lane.id}`,
		position: { x: LANE_LABEL_X, y: index * LANE_HEIGHT },
		data: { label: lane.name },
		class: 'lane-label',
		draggable: false,
		selectable: false,
		connectable: false
	}));
	const taskNodes: Node[] = workflow.nodes.map((node) => {
		const suffix = node.taskType ? ` (${labels.taskType(node.taskType)})` : '';
		return {
			id: node.id,
			position: { x: node.posX, y: node.posY },
			data: { label: node.title + suffix },
			class: `kind-${node.kind}`,
			type:
				node.kind === 'start_event' ? 'input' : node.kind === 'end_event' ? 'output' : 'default',
			draggable: false,
			connectable: false
		};
	});
	const edges: Edge[] = workflow.edges.map((edge) => ({
		id: edge.id,
		source: edge.source,
		target: edge.target,
		markerEnd: { type: 'arrowclosed' as MarkerType }
	}));
	return { nodes: [...laneNodes, ...taskNodes], edges };
}
