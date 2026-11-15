<script lang="ts">
	import type { Builders } from '#lib/api/execution.js';
	import BuilderChoice from '#lib/components/BuilderChoice.svelte';
	import {
		builderIds,
		builderOf,
		defaultReviewerHarness,
		noReviewer,
		reviewerAfterHarnessChange,
		reviewerConflict,
		type Reviewer
	} from '#lib/execution/builders.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	/** The optional second builder that runs the independent review steps. */
	let {
		builders,
		harness,
		model,
		reviewer,
		save,
		disabled
	}: {
		builders: Builders;
		/** The builder that writes the software and its model (an empty string is the default). */
		harness: string;
		model: string;
		reviewer: Reviewer;
		save: (next: Reviewer) => Promise<void>;
		disabled: boolean;
	} = $props();

	let draft = $derived<Reviewer>({ ...reviewer });
	const on = $derived(draft.harness !== '');
	const conflict = $derived(reviewerConflict({ harness, model }, draft));
	const chosen = $derived(builderOf(builders, draft.harness));

	async function change(next: Reviewer) {
		draft = next;
		if (!reviewerConflict({ harness, model }, next)) await save(next);
	}

	const toggle = (checked: boolean) =>
		change(checked ? { ...noReviewer, harness: defaultReviewerHarness(harness) } : noReviewer);
</script>

<div class="optional second" data-testid="second-opinion">
	<span class="optional-label">{locale.t('step.optional')}</span>
	<div class="body">
		<h4>{locale.t('build.second.heading')}</h4>
		<label class="inline">
			<input
				type="checkbox"
				role="switch"
				checked={on}
				{disabled}
				onchange={(e) => void toggle(e.currentTarget.checked)}
				data-testid="reviewer-switch"
			/>
			{locale.t('build.second.switch')}
		</label>
		{#if on}
			<p class="hint">{locale.t('build.second.lead')}</p>
			<div class="cards" role="radiogroup" aria-label={locale.t('build.second.who')}>
				{#each builderIds as id (id)}
					{@const builder = builderOf(builders, id)}
					{#if builder}
						<button
							type="button"
							class="pick"
							class:chosen={draft.harness === id}
							role="radio"
							aria-checked={draft.harness === id}
							{disabled}
							onclick={() => change(reviewerAfterHarnessChange(draft, id))}
							data-testid={`reviewer-pick-${id}`}
						>
							<strong>{locale.t(`build.${id}` as MessageKey)}</strong>
							<span class="state" class:ok={builder.ready} data-testid={`reviewer-state-${id}`}>
								{builder.ready ? locale.t('build.ready') : locale.t('build.notReady')}
							</span>
						</button>
					{/if}
				{/each}
			</div>
			{#if chosen}
				<BuilderChoice
					builder={chosen}
					model={draft.model}
					effort={draft.effort}
					save={(m, e) => change({ ...draft, model: m, effort: e })}
				/>
				{#each chosen.missing as item (item.code)}
					<p class="hint" data-testid={`reviewer-missing-${item.code}`}>
						{locale.has(`build.code.${item.code}`)
							? locale.t(`build.code.${item.code}` as MessageKey)
							: item.message}
					</p>
				{/each}
			{/if}
			{#if conflict}
				<p role="alert" data-testid="reviewer-conflict">{locale.t('build.second.same')}</p>
			{/if}
		{/if}
	</div>
</div>

<style>
	.second {
		align-items: flex-start;
	}
	.body {
		flex: 1 1 20rem;
		min-width: 0;
	}
	h4 {
		margin: 0 0 var(--space-1);
	}
	.cards {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-2);
		margin-top: var(--space-2);
	}
	.pick {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
		gap: var(--space-1);
		text-align: left;
		color: inherit;
		background: transparent;
		border: 1px solid var(--line);
		border-radius: var(--radius-sm);
		padding: var(--space-2) var(--space-3);
	}
	.pick.chosen {
		border-color: var(--blue);
		background: var(--blue-soft);
	}
	.state {
		color: var(--amber-ink);
		font-size: var(--text-sm);
		font-weight: 500;
	}
	.state.ok {
		color: var(--green-ink);
	}
</style>
