<script lang="ts">
	import { updateStepProfile, type StepDef, type StepProfile } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { EFFORTS, MAX_CAP, moveStep, stepProblem } from '#lib/settings/steps.js';

	let { profile, saved, cancel }: { profile: StepProfile; saved: () => void; cancel: () => void } =
		$props();

	// The editor works on a copy of the profile it is opened with.
	// svelte-ignore state_referenced_locally
	let name = $state(profile.name);
	// svelte-ignore state_referenced_locally
	let steps = $state<StepDef[]>(profile.steps.map((s) => ({ ...s })));
	const saving = createAction();
	const broken = $derived(steps.map(stepProblem));
	const canSave = $derived(name.trim() !== '' && broken.every((p) => p === null));

	async function save() {
		// An emptied number field is not a number: it means no cap.
		const cleaned = steps.map((s) => ({
			...s,
			capUsd: typeof s.capUsd === 'number' ? s.capUsd : null,
			effort: s.effort ? s.effort : null
		}));
		if (await saving.run(() => updateStepProfile(profile.id, name, cleaned))) saved();
	}
</script>

<form
	class="editor"
	data-testid="profile-editor"
	onsubmit={(e) => {
		e.preventDefault();
		void save();
	}}
>
	<label>
		{locale.t('steps.profileName')}
		<input bind:value={name} maxlength="60" data-testid="profile-name" />
	</label>
	{#each steps as step, i (step.key)}
		<fieldset data-testid="step-edit">
			<legend>{i + 1}. {steps[i]!.key}</legend>
			<label>
				{locale.t('steps.stepTitle')}
				<input bind:value={steps[i]!.title} maxlength="80" data-testid="step-title" />
			</label>
			<label>
				{locale.t('steps.prompt')}
				<textarea rows="4" bind:value={steps[i]!.prompt} maxlength="4000"></textarea>
			</label>
			<label>
				{locale.t('steps.cap')}
				<input
					type="number"
					min="0.1"
					max={MAX_CAP}
					step="0.1"
					placeholder={locale.t('steps.capNone')}
					bind:value={steps[i]!.capUsd}
					data-testid="step-cap"
				/>
			</label>
			<label>
				{locale.t('steps.effort')}
				<select
					value={steps[i]!.effort ?? ''}
					onchange={(e) => (steps[i]!.effort = e.currentTarget.value || null)}
					data-testid="step-effort"
				>
					<option value="">{locale.t('steps.effortProject')}</option>
					{#each EFFORTS as level (level)}
						<option value={level}>{level}</option>
					{/each}
				</select>
			</label>
			<label class="inline">
				<input type="checkbox" bind:checked={steps[i]!.gated} />
				{locale.t('steps.gated')}
			</label>
			<label class="inline">
				<input
					type="checkbox"
					bind:checked={steps[i]!.review}
					onchange={() => {
						if (!steps[i]!.review) steps[i]!.reviewerOnly = false;
					}}
					data-testid="step-review"
				/>
				{locale.t('steps.review')}
			</label>
			<label class="inline">
				<input
					type="checkbox"
					bind:checked={steps[i]!.reviewerOnly}
					disabled={!steps[i]!.review}
					data-testid="step-reviewer-only"
				/>
				{locale.t('steps.reviewerOnly')}
			</label>
			{#if broken[i]}<p role="alert">{locale.t(`steps.problem.${broken[i]}`)}</p>{/if}
			<button type="button" disabled={i === 0} onclick={() => (steps = moveStep(steps, i, -1))}>
				{locale.t('steps.up')}
			</button>
			<button
				type="button"
				disabled={i === steps.length - 1}
				onclick={() => (steps = moveStep(steps, i, 1))}
			>
				{locale.t('steps.down')}
			</button>
		</fieldset>
	{/each}
	<button
		type="submit"
		class="primary"
		disabled={!canSave || saving.busy}
		data-testid="profile-save"
	>
		{locale.t('list.save')}
	</button>
	<button type="button" onclick={cancel}>{locale.t('list.cancel')}</button>
	{#if saving.error}<p role="alert">{saving.error}</p>{/if}
</form>
