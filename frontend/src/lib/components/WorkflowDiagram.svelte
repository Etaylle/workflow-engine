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
		height: 28rem;
		border: 1px solid #ccc;
	}
	.diagram :global(.lane-label) {
		background: none;
		border: none;
		box-shadow: none;
		font-weight: 600;
		width: 9rem;
		text-align: right;
	}
	.diagram :global(.kind-start_event) {
		border-color: #2a7d3f;
	}
	.diagram :global(.kind-end_event) {
		border-color: #a33;
	}
</style>
