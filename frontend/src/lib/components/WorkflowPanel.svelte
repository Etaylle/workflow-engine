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
	import LimitsCard from './LimitsCard.svelte';
	import ReviewSummary from './ReviewSummary.svelte';
	import { reviewFacts } from '#lib/planning/review.js';
	import WorkflowDiagram from './WorkflowDiagram.svelte';
	import WorkflowEditor from './WorkflowEditor.svelte';
	import Waiting from './Waiting.svelte';
	import ErrorNote from './ErrorNote.svelte';
	import { isKeyProblem } from '#lib/ai/keyState.js';

	import { rank } from '#lib/planning/steps.js';
	import {
		getQuestions,
		getRequirements,
		type Question,
		type Requirements
	} from '#lib/api/client.js';

	let { project, changed }: { project: Project; changed: () => void } = $props();

	let workflow = $state<Workflow | null>(null);
	let contracts = $state<Contract[]>([]);
	let busy = $state<'idle' | 'loading' | 'generating' | 'confirming'>('loading');
	let error = $state<string | null>(null);
	let keyIssue = $state(false);
	let editing = $state(false);

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
		keyIssue = isKeyProblem(key);
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

	let questions = $state<Question[]>([]);
	let requirements = $state<Requirements>({ epics: [] });
	const reviewed = $derived(rank(project.status) >= 4);
	const facts = $derived(reviewFacts(questions, requirements, workflow));

	$effect(() => {
		void project.id;
		void project.status;
		error = null;
		void load();
		void Promise.all([getQuestions(project.id), getRequirements(project.id)]).then(([q, r]) => {
			questions = q;
			requirements = r;
		}, fail);
	});
</script>

<section class="step" aria-labelledby="workflow-heading" data-testid="step-workflow">
	<h2 id="workflow-heading">
		{locale.t(reviewed ? 'review.heading' : 'workflow.heading')}
	</h2>
	<p class="sub">{locale.t('step.workflowLead')}</p>
	{#if reviewed}
		<ReviewSummary {project} {facts} />
		<LimitsCard {project} />
	{/if}

	{#if !workflow && busy !== 'loading'}
		<div class="empty">
			<h3>{locale.t('workflow.emptyTitle')}</h3>
			<p>{locale.t('workflow.emptyStep')}</p>
			<button
				class="primary"
				onclick={() => run('generating', () => generateWorkflow(project.id))}
				disabled={busy !== 'idle'}
				data-testid="generate"
			>
				{busy === 'generating' ? locale.t('workflow.generating') : locale.t('workflow.generate')}
			</button>
		</div>
	{/if}
	{#if busy === 'generating'}<Waiting what={locale.t('workflow.generating')} />{/if}
	{#if error}<ErrorNote message={error} {keyIssue} testid="workflow-error" />{/if}

	{#if workflow}
		<WorkflowDiagram {workflow} />
		{#if editing}
			<WorkflowEditor
				{workflow}
				saved={() => {
					editing = false;
					void load();
					changed();
				}}
				cancel={() => (editing = false)}
			/>
		{/if}
		<div class="actions">
			{#if !editing}
				<button onclick={() => (editing = true)} data-testid="edit-workflow">
					{locale.t('workflow.edit')}
				</button>
			{/if}
			<button
				onclick={() => run('generating', () => generateWorkflow(project.id))}
				disabled={busy !== 'idle'}
				data-testid="generate"
			>
				{locale.t('workflow.regenerate')}
			</button>
		</div>
		{#if workflow.status === 'valid'}
			{#if workflow.confirmedContractVersion}
				<p>
					{locale.t('workflow.changedSince', {
						version: String(workflow.confirmedContractVersion)
					})}
				</p>
			{/if}
			<button
				class="primary"
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
	{/if}
</section>
