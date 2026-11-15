<script lang="ts">
	import { updateProject, type Project } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { effectiveBuildMode, type BuildMode } from '#lib/planning/buildMode.js';
	import { toasts } from '#lib/shell/toast.svelte.js';

	/** How the run builds: the whole software at once, or one task after the other. */
	let { project, software }: { project: Project; software: number } = $props();

	let chosen = $derived<BuildMode>(effectiveBuildMode(project.buildMode, software));
	const saving = createAction();

	async function choose(mode: BuildMode) {
		chosen = mode;
		const ok = await saving.run(() => updateProject(project.id, { buildMode: mode }));
		if (ok) toasts.show(locale.t('buildMode.savedToast'));
	}
</script>

<fieldset class="build-mode" data-testid="build-mode">
	<legend>{locale.t('buildMode.title')}</legend>
	{#each ['whole', 'tasks'] as const as mode (mode)}
		<label>
			<input
				type="radio"
				name="build-mode"
				value={mode}
				checked={chosen === mode}
				onchange={() => void choose(mode)}
				data-testid={`build-mode-${mode}`}
			/>
			{locale.t(`buildMode.${mode}`)}
		</label>
	{/each}
	{#if saving.error}<p role="alert">{saving.error}</p>{/if}
</fieldset>

<style>
	.build-mode {
		border: 0;
		padding: 0;
		margin: var(--space-2) 0 0;
	}
	.build-mode label {
		display: block;
	}
</style>
