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
	role="group"
	aria-label={locale.t('workflow.diagramLabel')}
	data-testid="diagram"
>
	{#key workflow.id + ':' + workflow.version}
		<SvelteFlow
			nodes={flow.nodes}
			edges={flow.edges}
			fitView
			colorMode="system"
			fitViewOptions={{ padding: 0.04, maxZoom: 1.8 }}
			minZoom={0.3}
			ariaLabelConfig={{
				'controls.zoomIn.ariaLabel': locale.t('diagram.zoomIn'),
				'controls.zoomOut.ariaLabel': locale.t('diagram.zoomOut'),
				'controls.fitView.ariaLabel': locale.t('diagram.fit')
			}}
			preventScrolling={false}
			zoomOnScroll={false}
			panOnScroll={false}
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
		height: clamp(18rem, calc(100vh - 20rem), 34rem);
		width: 100%;
		border: 1px solid var(--line);
		border-radius: var(--radius);
		background: var(--sunken);
	}
	.diagram :global(.lane-label) {
		background: var(--blue-soft);
		border: 1px solid var(--blue-line);
		color: var(--blue-ink);
		border-radius: var(--radius-sm);
	}
	.diagram :global(.lane-label) {
		display: flex;
		align-items: flex-start;
		justify-content: flex-start;
		padding: var(--space-2) var(--space-3);
		font-weight: 600;
		box-shadow: none;
		pointer-events: none;
	}
	.diagram :global(.lane-label .svelte-flow__handle) {
		display: none;
	}
	.diagram :global(.svelte-flow__node) {
		font-size: 1.05rem;
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
		border-color: var(--green-ink);
	}
	.diagram :global(.kind-end_event) {
		border-color: var(--red);
	}
</style>
