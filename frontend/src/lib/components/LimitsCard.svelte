<script lang="ts">
	import { getProjectSettings, updateProjectSettings, type Project } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { toasts } from '#lib/shell/toast.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { MAX_LIMIT, MIN_LIMIT, parseLimit } from '#lib/settings/limit.js';

	let { project }: { project: Project } = $props();

	let text = $state('');
	let defaultLimit = $state(10);
	let saved = $state(false);
	let baseline = $state('');
	const saving = createAction();
	const parsed = $derived(parseLimit(text));

	$effect(() => {
		void getProjectSettings(project.id).then(
			(s) => {
				defaultLimit = s.defaultCostLimitUsd;
				text = s.usesDefaultLimit ? '' : String(s.costLimitUsd);
				baseline = text;
			},
			() => saving.fail(locale.t('error.unreachable'))
		);
	});

	async function save() {
		if (parsed === 'invalid' || text === baseline) return;
		saved = false;
		const current = await getProjectSettings(project.id).catch(() => null);
		if (!current) return saving.fail(locale.t('error.unreachable'));
		saved = await saving.run(() =>
			updateProjectSettings(project.id, {
				stepProfileId: current.usesDefaultProfile ? null : current.stepProfileId,
				costLimitUsd: parsed
			})
		);
		if (saved) {
			baseline = text;
			toasts.show(locale.t('limits.savedToast'));
		}
	}
</script>

<form
	class="optional limits"
	data-testid="limits-card"
	onsubmit={(e) => {
		e.preventDefault();
		void save();
	}}
>
	<span class="optional-label">{locale.t('step.optional')}</span>
	<label>
		{locale.t('limits.cost')}
		<input
			bind:value={text}
			inputmode="decimal"
			placeholder={String(defaultLimit)}
			aria-invalid={parsed === 'invalid' ? 'true' : undefined}
			aria-describedby="limit-hint"
			data-testid="limit-input"
			onblur={() => void save()}
		/>
		<span class="hint" id="limit-hint">
			{locale.t('limits.hint', {
				default: String(defaultLimit),
				min: String(MIN_LIMIT),
				max: String(MAX_LIMIT)
			})}
		</span>
		{#if parsed === 'invalid'}
			<span class="field-error" role="alert" data-testid="limit-invalid">
				{locale.t('limits.invalid', { min: String(MIN_LIMIT), max: String(MAX_LIMIT) })}
			</span>
		{/if}
	</label>
	{#if saved}<p class="hint" role="status" data-testid="limit-saved">
			{locale.t('limits.saved')}
		</p>{/if}
	{#if saving.error}<p role="alert">{saving.error}</p>{/if}
</form>

<style>
	.limits {
		display: block;
	}
	.limits label {
		margin: var(--space-2) 0 0;
	}
	.limits input {
		max-width: 12rem;
	}
</style>
