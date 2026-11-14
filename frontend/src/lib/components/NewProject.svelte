<script lang="ts">
	import { createProject, type Project } from '#lib/api/client.js';
	import { describeError } from '#lib/api/problem.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	let { created }: { created: (project: Project) => void } = $props();

	let wish = $state('');
	let apiKey = $state('');
	let busy = $state(false);
	let error = $state<string | null>(null);

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		if (wish.trim() === '') {
			error = locale.t('project.wishMissing');
			return;
		}
		busy = true;
		error = null;
		try {
			const project = await createProject(wish, apiKey);
			wish = '';
			apiKey = '';
			created(project);
		} catch (e) {
			const { key, params } = describeError(e);
			error = locale.t(key, params);
		} finally {
			busy = false;
		}
	}
</script>

<section aria-labelledby="new-heading">
	<h2 id="new-heading">{locale.t('project.heading')}</h2>
	<form onsubmit={submit}>
		<label>
			{locale.t('project.wishLabel')}
			<textarea rows="4" bind:value={wish} data-testid="wish"></textarea>
		</label>
		<label>
			{locale.t('project.keyLabel')}
			<input type="password" autocomplete="off" bind:value={apiKey} data-testid="project-key" />
		</label>
		<button type="submit" disabled={busy}>
			{busy ? locale.t('project.creating') : locale.t('project.create')}
		</button>
	</form>
	{#if error}<p role="alert">{error}</p>{/if}
</section>

<style>
	label,
	textarea,
	input {
		display: block;
		width: 100%;
		box-sizing: border-box;
		margin: 0.25rem 0 0.5rem;
	}
</style>
