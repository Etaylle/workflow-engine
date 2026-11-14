<script lang="ts">
	import { untrack } from 'svelte';
	import { editWorkflow, type Workflow } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import {
		addAfter,
		addEdge,
		addLane,
		fromWorkflow,
		removeEdge,
		removeNode,
		toRequest,
		type Draft
	} from '#lib/workflow/edit.js';

	let { workflow, saved, cancel }: { workflow: Workflow; saved: () => void; cancel: () => void } =
		$props();

	let draft = $state<Draft>(untrack(() => fromWorkflow(workflow)));
	let newLane = $state('');
	let from = $state('');
	let to = $state('');
	const saving = createAction();

	const titleOf = (key: string) => draft.nodes.find((n) => n.key === key)?.title ?? key;

	async function save() {
		const ok = await saving.run(() => editWorkflow(workflow.projectId, toRequest(draft)));
		if (ok) saved();
	}
</script>

<section class="card" data-testid="workflow-editor">
	<h3>{locale.t('workflow.editHeading')}</h3>
	<p class="lead">{locale.t('workflow.editHelp')}</p>

	<ol class="steps">
		{#each draft.nodes as node, index (node.key)}
			<li class="card" data-testid={`step-${index + 1}`}>
				<label>
					{locale.t('workflow.stepTitle')}
					<input type="text" bind:value={node.title} data-testid={`step-title-${index + 1}`} />
				</label>
				{#if node.kind === 'task'}
					<label>
						{locale.t('workflow.stepWhat')}
						<input type="text" bind:value={node.description} />
					</label>
				{/if}
				<div class="row">
					<label>
						{locale.t('workflow.stepLane')}
						<select bind:value={node.lane}>
							{#each draft.lanes as lane (lane)}<option value={lane}>{lane}</option>{/each}
						</select>
					</label>
					{#if node.kind === 'task'}
						<label>
							{locale.t('workflow.stepType')}
							<select bind:value={node.taskType}>
								<option value="software">{locale.t('workflow.taskSoftware')}</option>
								<option value="manual">{locale.t('workflow.taskManual')}</option>
							</select>
						</label>
					{:else}
						<span class="pill">
							{locale.t(node.kind === 'start_event' ? 'workflow.kindStart' : 'workflow.kindEnd')}
						</span>
					{/if}
				</div>
				<button
					type="button"
					disabled={node.kind === 'end_event'}
					onclick={() =>
						(draft = addAfter(draft, node.key, crypto.randomUUID(), locale.t('workflow.newStep')))}
					data-testid={`add-after-${index + 1}`}
				>
					{locale.t('workflow.addAfter')}
				</button>
				<button
					type="button"
					class="danger"
					disabled={node.kind === 'start_event'}
					onclick={() => (draft = removeNode(draft, node.key))}
					data-testid={`delete-step-${index + 1}`}
				>
					{locale.t('list.delete')}
				</button>
			</li>
		{/each}
	</ol>

	<h4>{locale.t('workflow.lanes')}</h4>
	<form
		class="row"
		onsubmit={(e) => {
			e.preventDefault();
			draft = addLane(draft, newLane);
			newLane = '';
		}}
	>
		<span>{draft.lanes.join(', ')}</span>
		<input type="text" bind:value={newLane} aria-label={locale.t('workflow.newLane')} />
		<button type="submit">{locale.t('workflow.newLane')}</button>
	</form>

	<h4>{locale.t('workflow.arrows')}</h4>
	<ul>
		{#each draft.edges as edge (`${edge.from}>${edge.to}`)}
			<li>
				{titleOf(edge.from)} &rarr; {titleOf(edge.to)}
				<button type="button" onclick={() => (draft = removeEdge(draft, edge.from, edge.to))}>
					{locale.t('list.delete')}
				</button>
			</li>
		{/each}
	</ul>
	<div class="row">
		<select bind:value={from} aria-label={locale.t('workflow.arrowFrom')}>
			<option value="">{locale.t('workflow.arrowFrom')}</option>
			{#each draft.nodes as node (node.key)}<option value={node.key}>{node.title}</option>{/each}
		</select>
		<select bind:value={to} aria-label={locale.t('workflow.arrowTo')}>
			<option value="">{locale.t('workflow.arrowTo')}</option>
			{#each draft.nodes as node (node.key)}<option value={node.key}>{node.title}</option>{/each}
		</select>
		<button
			type="button"
			disabled={from === '' || to === ''}
			onclick={() => (draft = addEdge(draft, from, to))}
		>
			{locale.t('workflow.addArrow')}
		</button>
	</div>

	<p>
		<button
			type="button"
			class="primary"
			onclick={save}
			disabled={saving.busy}
			data-testid="editor-save-workflow"
		>
			{locale.t('workflow.saveEdit')}
		</button>
		<button type="button" onclick={cancel}>{locale.t('list.cancel')}</button>
	</p>
	{#if saving.error}<p role="alert" data-testid="workflow-edit-error">{saving.error}</p>{/if}
</section>

<style>
	.steps {
		list-style: none;
		padding: 0;
	}
	.row {
		display: flex;
		flex-wrap: wrap;
		gap: 0.75rem;
		align-items: center;
		margin-bottom: 0.5rem;
	}
</style>
