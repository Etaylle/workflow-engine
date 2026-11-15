<script lang="ts">
	import { getProjectSettings, updateProjectSettings, type Project } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { MAX_LIMIT, MIN_LIMIT, parseLimit } from '#lib/settings/limit.js';

	let { project }: { project: Project } = $props();

	let text = $state('');
	let defaultLimit = $state(10);
	let saved = $state(false);
	const saving = createAction();
	const parsed = $derived(parseLimit(text));

	$effect(() => {
		void getProjectSettings(project.id).then(
			(s) => {
				defaultLimit = s.defaultCostLimitUsd;
				text = s.usesDefaultLimit ? '' : String(s.costLimitUsd);
			},
			() => saving.fail(locale.t('error.unreachable'))
		);
	});

	async function save() {
		if (parsed === 'invalid') return;
		saved = false;
		const current = await getProjectSettings(project.id).catch(() => null);
		if (!current) return saving.fail(locale.t('error.unreachable'));
		saved = await saving.run(() =>
			updateProjectSettings(project.id, {
				stepProfileId: current.usesDefaultProfile ? null : current.stepProfileId,
				costLimitUsd: parsed
			})
		);
	}
</script>

<div class="card" data-testid="limits-card">
	<h3>{locale.t('limits.title')}</h3>
	<form
		onsubmit={(e) => {
			e.preventDefault();
			void save();
		}}
	>
		<label>
			{locale.t('limits.cost')}
			<input
				bind:value={text}
				inputmode="decimal"
				placeholder={String(defaultLimit)}
				data-testid="limit-input"
				oninput={() => (saved = false)}
			/>
		</label>
		<p class="lead">
			{locale.t('limits.hint', {
				default: String(defaultLimit),
				min: String(MIN_LIMIT),
				max: String(MAX_LIMIT)
			})}
		</p>
		<button type="submit" disabled={parsed === 'invalid' || saving.busy} data-testid="limit-save">
			{locale.t('list.save')}
		</button>
		{#if parsed === 'invalid'}<p role="alert" data-testid="limit-invalid">
				{locale.t('limits.invalid', { min: String(MIN_LIMIT), max: String(MAX_LIMIT) })}
			</p>{/if}
		{#if saved}<p role="status" data-testid="limit-saved">{locale.t('limits.saved')}</p>{/if}
		{#if saving.error}<p role="alert">{saving.error}</p>{/if}
	</form>
</div>
