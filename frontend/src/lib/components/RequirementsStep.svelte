<script lang="ts">
	import {
		generateRequirements,
		getRequirements,
		type Project,
		type Requirements
	} from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import Waiting from './Waiting.svelte';
	import AutoStartNote from './AutoStartNote.svelte';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { claimAutoStart, releaseAutoStart, shouldAutoStart } from '#lib/planning/autoStart.js';
	import ErrorNote from './ErrorNote.svelte';
	import { rank } from '#lib/planning/steps.js';
	import { stepHash } from '#lib/shell/route.js';
	import StepShell from './StepShell.svelte';

	let {
		project,
		changed,
		back
	}: {
		project: Project;
		changed: () => Promise<void>;
		back: string;
	} = $props();

	const priorityKey = (priority: string) => `priority.${priority}` as MessageKey;

	let requirements = $state<Requirements>({ epics: [] });
	const writing = createAction();
	const loading = createAction();
	const written = $derived(requirements.epics.length > 0);
	const editable = $derived(rank(project.status) <= 3);

	async function load() {
		const ok = await loading.run(async () => (requirements = await getRequirements(project.id)));
		if (ok) didLoad = true;
	}

	let didLoad = $state(false);
	const keyMissing = $derived(engineKey.keys !== null && engineKey.keys.planning === null);

	$effect(() => {
		const go = shouldAutoStart({
			loaded: didLoad,
			missingContent: !written,
			stateAllows: project.status === 'answered',
			busy: writing.busy || loading.busy,
			failed: writing.error !== null,
			keyReady: engineKey.keys === null ? null : engineKey.keys.planning !== null
		});
		if (go && claimAutoStart(project.id, 'requirements')) void write();
	});

	async function write() {
		const ok = await writing.run(
			async () => (requirements = await generateRequirements(project.id))
		);
		if (ok) {
			releaseAutoStart(project.id, 'requirements');
			await changed();
		}
	}

	$effect(() => {
		void project.id;
		void project.status;
		void load();
	});
</script>

<StepShell
	step="requirements"
	heading={locale.t('requirements.heading')}
	lead={locale.t('step.requirementsLead')}
	{back}
>
	{#if loading.error}<p role="alert">{loading.error}</p>{/if}

	{#if loading.busy && !written}
		<div class="skeleton" aria-hidden="true"></div>
	{:else if !written && editable}
		<div class="empty">
			<h3>{locale.t('requirements.emptyTitle')}</h3>
			<p>{locale.t('requirements.emptyStep')}</p>
		</div>
	{/if}
	{#if writing.busy}<Waiting what={locale.t('requirements.writing')} />{/if}
	{#if !written}
		<AutoStartNote
			error={writing.error}
			keyIssue={writing.keyIssue}
			{keyMissing}
			busy={writing.busy}
			retry={() => void write()}
			testid="requirements-error"
		/>
	{:else if writing.error}
		<ErrorNote message={writing.error} keyIssue={writing.keyIssue} testid="requirements-error" />
	{/if}

	{#if written}
		<p class="count">
			{locale.t('requirements.count', {
				count: String(requirements.epics.reduce((n, e) => n + e.requirements.length, 0)),
				epics: String(requirements.epics.length)
			})}
		</p>
		{#each requirements.epics as epic (epic.key)}
			<h3 class="epic"><span class="badge">{epic.key}</span> {epic.name}</h3>
			<ul data-testid="requirements">
				{#each epic.requirements as requirement (requirement.key)}
					<li>
						<strong>{requirement.key}</strong>
						<span class="badge" class:green={requirement.priority === 'must'}>
							{locale.t(priorityKey(requirement.priority))}
						</span>
						<span class="text">{requirement.text}</span>
					</li>
				{/each}
			</ul>
		{/each}
		{#if editable}
			<div class="optional">
				<span class="optional-label">{locale.t('step.optional')}</span>
				<button
					class="quiet"
					onclick={write}
					disabled={writing.busy}
					data-testid="write-requirements-again"
				>
					{writing.busy ? locale.t('requirements.writing') : locale.t('requirements.again')}
				</button>
			</div>
		{/if}
	{/if}

	{#snippet primary()}
		{#if written}
			<a
				class="button primary"
				href={stepHash(project.id, 'workflow')}
				data-testid="requirements-continue"
			>
				{locale.t('step.looksGood')}
			</a>
		{:else}
			<button class="primary" disabled data-testid="requirements-continue">
				{locale.t('step.looksGood')}
			</button>
		{/if}
	{/snippet}
</StepShell>

<style>
	.count {
		color: var(--muted);
		margin: 0 0 0.5rem;
	}
	.epic {
		font-size: 1.05rem;
		margin: 1.25rem 0 0.4rem;
	}
	ul {
		list-style: none;
		margin: 0;
		padding: 0;
		border: 1px solid var(--line);
		border-radius: 10px;
	}
	li {
		display: grid;
		grid-template-columns: 5.5rem auto 1fr;
		gap: 0.6rem;
		align-items: baseline;
		padding: 0.6rem 0.9rem;
	}
	li + li {
		border-top: 1px solid var(--line);
	}
	@media (max-width: 40rem) {
		li {
			grid-template-columns: 1fr;
			gap: 0.2rem;
		}
	}
</style>
