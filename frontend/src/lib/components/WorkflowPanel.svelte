<script lang="ts">
	import {
		confirmWorkflow,
		generateWorkflow,
		getWorkflow,
		listContracts,
		type Contract,
		type Project,
		type Workflow
	} from '#lib/api/client.js';
	import { describeError } from '#lib/api/problem.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import WorkflowDiagram from './WorkflowDiagram.svelte';

	let { project, changed }: { project: Project; changed: () => void } = $props();

	let workflow = $state<Workflow | null>(null);
	let contracts = $state<Contract[]>([]);
	let busy = $state<'idle' | 'loading' | 'generating' | 'confirming'>('loading');
	let error = $state<string | null>(null);

	async function load() {
		busy = 'loading';
		try {
			[workflow, contracts] = await Promise.all([
				getWorkflow(project.id),
				listContracts(project.id)
			]);
		} catch (e) {
			fail(e);
		} finally {
			busy = 'idle';
		}
	}

	function fail(e: unknown) {
		const { key, params } = describeError(e);
		error = locale.t(key, params);
	}

	async function run(kind: 'generating' | 'confirming', action: () => Promise<unknown>) {
		busy = kind;
		error = null;
		try {
			await action();
			await load();
			changed();
		} catch (e) {
			fail(e);
			busy = 'idle';
		}
	}

	$effect(() => {
		void project.id;
		error = null;
		void load();
	});
</script>

<section aria-labelledby="workflow-heading">
	<h2 id="workflow-heading">{locale.t('workflow.heading', { name: project.name })}</h2>
	<p>{project.wish}</p>

	<button
		onclick={() => run('generating', () => generateWorkflow(project.id))}
		disabled={busy !== 'idle'}
		data-testid="generate"
	>
		{workflow ? locale.t('workflow.regenerate') : locale.t('workflow.generate')}
	</button>
	{#if busy === 'generating'}<p role="status">{locale.t('workflow.generating')}</p>{/if}
	{#if error}<p role="alert" data-testid="workflow-error">{error}</p>{/if}

	{#if workflow}
		<WorkflowDiagram {workflow} />
		{#if workflow.status === 'valid'}
			{#if workflow.confirmedContractVersion}
				<p>
					{locale.t('workflow.changedSince', {
						version: String(workflow.confirmedContractVersion)
					})}
				</p>
			{/if}
			<button
				onclick={() => run('confirming', () => confirmWorkflow(project.id))}
				disabled={busy !== 'idle'}
				data-testid="confirm"
			>
				{busy === 'confirming' ? locale.t('workflow.confirming') : locale.t('workflow.confirm')}
			</button>
		{:else}
			<p role="status" data-testid="confirmed">
				{locale.t('workflow.confirmedAs', {
					version: String(workflow.confirmedContractVersion ?? '')
				})}
			</p>
		{/if}
		{#if contracts.length > 0}
			<p data-testid="versions">
				{locale.t('workflow.versions', { versions: contracts.map((c) => c.version).join(', ') })}
			</p>
		{/if}
	{:else if busy === 'idle'}
		<p>{locale.t('workflow.none')}</p>
	{/if}
</section>
