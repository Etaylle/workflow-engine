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
	import AutoStartNote from './AutoStartNote.svelte';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { claimAutoStart, releaseAutoStart, shouldAutoStart } from '#lib/planning/autoStart.js';
	import { isKeyProblem } from '#lib/ai/keyState.js';

	import { stepHash } from '#lib/shell/route.js';
	import StepShell from './StepShell.svelte';
	import {
		getQuestions,
		getRequirements,
		type Question,
		type Requirements
	} from '#lib/api/client.js';

	let {
		project,
		changed,
		step,
		back
	}: {
		project: Project;
		changed: () => Promise<void>;
		step: 'workflow' | 'review';
		back: string;
	} = $props();
	const reviewing = $derived(step === 'review');

	let workflow = $state<Workflow | null>(null);
	let contracts = $state<Contract[]>([]);
	let busy = $state<'idle' | 'loading' | 'generating' | 'confirming'>('loading');
	let error = $state<string | null>(null);
	let keyIssue = $state(false);
	let editing = $state(false);
	let didLoad = $state(false);
	const keyMissing = $derived(engineKey.keys !== null && engineKey.keys.planning === null);

	$effect(() => {
		const go = shouldAutoStart({
			loaded: didLoad,
			missingContent: workflow === null,
			stateAllows: step === 'workflow' && project.status === 'requirements_ready',
			busy: busy !== 'idle',
			failed: error !== null,
			keyReady: engineKey.keys === null ? null : engineKey.keys.planning !== null
		});
		if (go && claimAutoStart(project.id, 'workflow')) {
			void run('generating', () => generateWorkflow(project.id));
		}
	});

	async function load() {
		busy = 'loading';
		try {
			[workflow, contracts] = await Promise.all([
				getWorkflow(project.id),
				listContracts(project.id)
			]);
			didLoad = true;
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
			releaseAutoStart(project.id, 'workflow');
			await load();
			await changed();
		} catch (e) {
			fail(e);
			busy = 'idle';
		}
	}

	let questions = $state<Question[]>([]);
	let requirements = $state<Requirements>({ epics: [] });
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

<StepShell
	{step}
	heading={reviewing ? locale.t('review.heading') : locale.t('workflow.heading')}
	lead={reviewing ? locale.t('step.reviewLead') : locale.t('step.workflowLead')}
	{back}
>
	{#if reviewing}
		<ReviewSummary {project} {facts} />
	{/if}

	{#if !workflow && busy !== 'loading'}
		<div class="empty">
			<h3>{locale.t('workflow.emptyTitle')}</h3>
			<p>{locale.t('workflow.emptyStep')}</p>
		</div>
	{/if}
	{#if busy === 'generating'}<Waiting what={locale.t('workflow.generating')} />{/if}
	{#if busy === 'confirming'}<Waiting what={locale.t('workflow.confirming')} />{/if}
	{#if !workflow}
		<AutoStartNote
			{error}
			{keyIssue}
			{keyMissing}
			busy={busy !== 'idle'}
			retry={() => void run('generating', () => generateWorkflow(project.id))}
			testid="workflow-error"
		/>
	{:else if error}
		<ErrorNote message={error} {keyIssue} testid="workflow-error" />
	{/if}

	{#if workflow}
		<WorkflowDiagram {workflow} />
		{#if editing}
			<WorkflowEditor
				{workflow}
				saved={() => {
					editing = false;
					void load();
					void changed();
				}}
				cancel={() => (editing = false)}
			/>
		{/if}
		{#if reviewing}
			<LimitsCard
				{project}
				software={workflow.nodes.filter((n) => n.kind === 'task' && n.taskType === 'software')
					.length}
			/>
			{#if workflow.status === 'valid' && workflow.confirmedContractVersion}
				<p class="notice">
					{locale.t('workflow.changedSince', {
						version: String(workflow.confirmedContractVersion)
					})}
				</p>
			{/if}
			{#if workflow.status !== 'valid'}
				<p class="confirmed" role="status" data-testid="confirmed">
					{locale.t('workflow.confirmedAs', {
						version: String(workflow.confirmedContractVersion ?? '')
					})}
					{locale.t('review.confirmedNext')}
				</p>
			{/if}
			{#if contracts.length > 0}
				<p class="hint" data-testid="versions">
					{locale.t('workflow.versions', { versions: contracts.map((c) => c.version).join(', ') })}
				</p>
			{/if}
		{/if}
		{#if !editing}
			<div class="optional">
				<span class="optional-label">{locale.t('step.optional')}</span>
				<button class="quiet" onclick={() => (editing = true)} data-testid="edit-workflow">
					{locale.t('workflow.edit')}
				</button>
				{#if !reviewing}
					<button
						class="quiet"
						onclick={() => run('generating', () => generateWorkflow(project.id))}
						disabled={busy !== 'idle'}
						data-testid="generate-again"
					>
						{locale.t('workflow.regenerate')}
					</button>
				{/if}
			</div>
		{/if}
	{/if}

	{#snippet primary()}
		{#if !workflow}
			<button class="primary" disabled data-testid="workflow-continue">
				{locale.t('step.looksGood')}
			</button>
		{:else if !reviewing}
			<a
				class="button primary"
				href={stepHash(project.id, 'review')}
				data-testid="workflow-continue"
			>
				{locale.t('step.looksGood')}
			</a>
		{:else if workflow.status === 'valid'}
			<button
				class="primary"
				onclick={() => run('confirming', () => confirmWorkflow(project.id))}
				disabled={busy !== 'idle' || editing}
				data-testid="confirm"
			>
				{busy === 'confirming' ? locale.t('workflow.confirming') : locale.t('workflow.confirm')}
			</button>
		{:else}
			<a class="button primary" href={stepHash(project.id, 'run')} data-testid="review-continue">
				{locale.t('step.continueRun')}
			</a>
		{/if}
	{/snippet}
</StepShell>

<style>
	.confirmed {
		color: var(--green-ink);
		background: var(--green-soft);
		border: 1px solid var(--green-line);
		border-radius: var(--radius-sm);
		padding: var(--space-3) var(--space-4);
	}
</style>
