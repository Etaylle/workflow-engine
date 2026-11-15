<script lang="ts">
	import { createProject, updateProject, type Project } from '#lib/api/client.js';
	import { describeError } from '#lib/api/problem.js';
	import { getBuilders, type Builders } from '#lib/api/execution.js';
	import { choiceAfterBuilderChange } from '#lib/execution/builders.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import BuildWith from '#lib/components/BuildWith.svelte';
	import { locale } from '#lib/i18n/state.svelte.js';

	const MAX_WISH = 4000;

	let { created }: { created: (project: Project) => void } = $props();

	let name = $state('');
	let wish = $state('');
	let harness = $state('claude_code');
	let builderModel = $state('');
	let builderEffort = $state('');
	let builders = $state<Builders | null>(null);
	let busy = $state(false);
	let error = $state<string | null>(null);
	let wishError = $state<string | null>(null);
	let wishField = $state<HTMLTextAreaElement>();
	const planning = $derived(engineKey.keys?.planning ?? null);

	$effect(() => {
		void engineKey.refresh();
		void getBuilders().then((fresh) => (builders = fresh));
	});

	async function chooseBuilder(id: string) {
		({ model: builderModel, effort: builderEffort } = choiceAfterBuilderChange(harness, id, {
			model: builderModel,
			effort: builderEffort
		}));
		harness = id;
	}

	async function saveChoice(model: string, effort: string) {
		builderModel = model;
		builderEffort = effort;
	}

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		if (wish.trim() === '') {
			wishError = locale.t('project.wishMissing');
			wishField?.focus();
			return;
		}
		busy = true;
		error = null;
		wishError = null;
		try {
			let project = await createProject(wish, '', name);
			if (harness !== project.agentHarness || builderModel || builderEffort) {
				project = await updateProject(project.id, {
					agentHarness: harness,
					builderModel,
					builderEffort
				});
			}
			created(project);
		} catch (e) {
			const { key, params } = describeError(e);
			error = locale.t(key, params);
		} finally {
			busy = false;
		}
	}
</script>

<form class="page" onsubmit={submit}>
	<a href="#projects">{locale.t('nav.back')}</a>
	<h1>{locale.t('wish.title')}</h1>
	<p class="lead">{locale.t('wish.lead')}</p>

	<div class="grid">
		<div class="card">
			<label>
				{locale.t('wish.name')}
				<input
					type="text"
					maxlength="120"
					bind:value={name}
					aria-describedby="name-hint"
					data-testid="name"
				/>
				<span class="hint" id="name-hint">{locale.t('wish.nameHint')}</span>
			</label>
			<label>
				{locale.t('wish.wishLabel')}
				<textarea
					rows="9"
					maxlength={MAX_WISH}
					bind:value={wish}
					bind:this={wishField}
					aria-invalid={wishError ? 'true' : undefined}
					aria-describedby="wish-hint wish-error"
					data-testid="wish"></textarea>
				<span class="hint" id="wish-hint">
					{locale.t('wish.wishHint')}
					{locale.t('wish.chars', { count: String(wish.length), max: String(MAX_WISH) })}
				</span>
				<span id="wish-error" role="alert" class="field-error" hidden={!wishError}>{wishError}</span
				>
			</label>
		</div>

		<aside class="card">
			<h2>{locale.t('wish.aiHeading')}</h2>
			<p class={planning ? 'planning' : 'planning warn'} data-testid="planning-line">
				{#if planning}
					{locale.t('wish.planningLine', {
						name: engineKey.nameOf(locale.t('key.nameAnthropic'), planning.provider),
						model: planning.model
					})}
				{:else}
					{locale.t('wish.planningNone')}
				{/if}
			</p>
			<button type="button" onclick={() => engineKey.openPanel()} data-testid="open-key-panel">
				{locale.t('wish.openKeyPanel')}
			</button>
		</aside>
	</div>

	{#if builders}
		<BuildWith
			{harness}
			model={builderModel}
			effort={builderEffort}
			{saveChoice}
			{builders}
			update={(fresh) => (builders = fresh)}
			choose={chooseBuilder}
		/>
	{/if}

	{#if error}<p role="alert">{error}</p>{/if}
	<footer>
		<span class="lead">{locale.t('wish.next')}</span>
		<button type="submit" class="primary" disabled={busy} data-testid="create">
			{busy ? locale.t('project.creating') : locale.t('wish.send')}
		</button>
	</footer>
</form>

<style>
	.grid {
		display: grid;
		grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
		gap: var(--space-5);
		align-items: start;
	}
	@media (max-width: 52rem) {
		.grid {
			grid-template-columns: minmax(0, 1fr);
		}
	}
	.planning {
		margin: 0 0 var(--space-3);
	}
	.warn {
		color: var(--red-ink, inherit);
	}
	footer {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		align-items: center;
		gap: var(--space-4);
		border-top: 1px solid var(--line);
		padding-top: var(--space-4);
	}
</style>
