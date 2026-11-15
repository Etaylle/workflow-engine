<script lang="ts">
	import type { Builder } from '#lib/api/execution.js';
	import { modelToSave } from '#lib/execution/builders.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	/** Model (and for Claude Code effort) of one builder; an empty value is the default. */
	let {
		builder,
		model,
		effort,
		save
	}: {
		builder: Builder;
		model: string;
		effort: string;
		save: (model: string, effort: string) => Promise<void>;
	} = $props();

	let typed = $derived(model);
	let invalid = $state(false);

	const hintOf = (id: string, fallback: string) =>
		locale.has(`build.modelHint.${id}`)
			? locale.t(`build.modelHint.${id}` as MessageKey)
			: fallback;

	async function commitTyped() {
		const value = modelToSave(typed);
		invalid = value === null;
		if (value !== null && value !== model) await save(value, effort);
	}
</script>

<div class="choice" data-testid={`choice-${builder.id}`}>
	<label>
		<span>{locale.t('build.modelLabel')}</span>
		{#if builder.customModel}
			<input
				type="text"
				list={`models-${builder.id}`}
				maxlength="80"
				bind:value={typed}
				placeholder={locale.t('build.defaultModel', { model: builder.model })}
				aria-invalid={invalid ? 'true' : undefined}
				onchange={commitTyped}
				onkeydown={(e) => e.key === 'Enter' && (e.preventDefault(), void commitTyped())}
				data-testid="builder-model"
			/>
			<datalist id={`models-${builder.id}`}>
				{#each builder.models as option (option.id)}
					<option value={option.id}>{hintOf(option.id, option.hint)}</option>
				{/each}
			</datalist>
			{#if invalid}<span class="field-error" role="alert">{locale.t('build.modelInvalid')}</span
				>{/if}
		{:else}
			<select
				value={model}
				onchange={(e) => void save(e.currentTarget.value, effort)}
				data-testid="builder-model"
			>
				<option value="">{locale.t('build.defaultModel', { model: builder.model })}</option>
				{#each builder.models as option (option.id)}
					<option value={option.id}>{option.id} - {hintOf(option.id, option.hint)}</option>
				{/each}
			</select>
		{/if}
	</label>
	{#if builder.efforts.length > 0}
		<label>
			<span>{locale.t('build.effortLabel')}</span>
			<select
				value={effort}
				onchange={(e) => void save(model, e.currentTarget.value)}
				data-testid="builder-effort"
			>
				<option value="">{locale.t('build.defaultEffort')}</option>
				{#each builder.efforts as level (level)}
					<option value={level}>{level}</option>
				{/each}
			</select>
		</label>
	{/if}
</div>

<style>
	.choice {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-2) var(--space-3);
		margin-top: var(--space-3);
		border-top: 1px dashed var(--line);
		padding-top: var(--space-2);
	}
	label {
		display: flex;
		flex-direction: column;
		gap: var(--space-1);
		font-size: var(--text-sm);
		min-width: 0;
		flex: 1 1 10rem;
	}
	select,
	input {
		max-width: 100%;
	}
</style>
