<script lang="ts">
	import { Background, Controls, SvelteFlow } from '@xyflow/svelte';
	import '@xyflow/svelte/dist/style.css';
	import type { Workflow } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { toFlow } from '#lib/workflow/flow.js';

	let { workflow }: { workflow: Workflow } = $props();

	const flow = $derived(
		toFlow(workflow, {
			taskType: (type) =>
				locale.t(type === 'software' ? 'workflow.taskSoftware' : 'workflow.taskManual')
		})
	);
</script>

<div
	class="diagram"
	role="img"
	aria-label={locale.t('workflow.diagramLabel')}
	data-testid="diagram"
>
	{#key workflow.id + ':' + workflow.version}
		<SvelteFlow
			nodes={flow.nodes}
			edges={flow.edges}
			fitView
			fitViewOptions={{ padding: 0.04, maxZoom: 1.4 }}
			nodesDraggable={false}
			nodesConnectable={false}
			elementsSelectable={false}
			proOptions={{ hideAttribution: true }}
		>
			<Background />
			<Controls showLock={false} />
		</SvelteFlow>
	{/key}
</div>

<style>
	.diagram {
		height: clamp(20rem, 50vh, 30rem);
		width: 100%;
		border: 1px solid var(--line);
		border-radius: 10px;
		background: var(--sunken);
	}
	.diagram :global(.lane-label) {
		background: rgba(100, 120, 160, 0.08);
		border: 1px solid #c8d0e0;
		border-radius: 4px;
	}
	.diagram :global(.lane-label) {
		display: flex;
		align-items: flex-start;
		justify-content: flex-start;
		padding: 0.4rem 0.6rem;
		font-weight: 600;
		box-shadow: none;
		pointer-events: none;
	}
	.diagram :global(.lane-label .svelte-flow__handle) {
		display: none;
	}
	.diagram :global(.svelte-flow__node) {
		background: var(--surface);
		color: var(--ink);
		border-color: var(--field-line);
	}
	.diagram :global(.svelte-flow__controls-button) {
		background: var(--surface);
		color: var(--ink);
		fill: var(--ink);
		border-color: var(--line);
	}
	.diagram :global(.kind-start_event) {
		border-color: #2a7d3f;
	}
	.diagram :global(.kind-end_event) {
		border-color: #a33;
	}
</style>
