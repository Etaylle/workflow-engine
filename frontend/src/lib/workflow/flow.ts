import { Position, type Edge, type MarkerType, type Node } from '@xyflow/svelte';
import type { Workflow } from '#lib/api/client.js';

const BAND_X = -200;
const LANE_HEIGHT = 180;
const BAND_TOP = -50;
const BAND_HEIGHT = 140;
const NODE_WIDTH = 160;
const BAND_PADDING = 60;

export type FlowLabels = { taskType: (type: 'software' | 'manual') => string };

/** Turns the API workflow into Svelte Flow nodes and edges (positions come from the engine). */
export function toFlow(workflow: Workflow, labels: FlowLabels): { nodes: Node[]; edges: Edge[] } {
	const right = Math.max(0, ...workflow.nodes.map((node) => node.posX)) + NODE_WIDTH + BAND_PADDING;
	const laneNodes: Node[] = workflow.lanes.map((lane, index) => ({
		id: `lane-${lane.id}`,
		position: { x: BAND_X, y: index * LANE_HEIGHT + BAND_TOP },
		data: { label: lane.name },
		style: `width: ${right - BAND_X}px; height: ${BAND_HEIGHT}px;`,
		class: 'lane-label',
		zIndex: -1,
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
			sourcePosition: Position.Right,
			targetPosition: Position.Left,
			draggable: false,
			connectable: false
		};
	});
	const edges: Edge[] = workflow.edges.map((edge) => ({
		id: edge.id,
		source: edge.source,
		target: edge.target,
		type: 'smoothstep',
		markerEnd: { type: 'arrowclosed' as MarkerType }
	}));
	return { nodes: [...laneNodes, ...taskNodes], edges };
}
