<script lang="ts">
	import { updateProject, type Project } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	let { project, changed }: { project: Project; changed: () => void } = $props();

	let editing = $state(false);
	let text = $state('');
	const saving = createAction();

	function start() {
		text = project.wish;
		editing = true;
	}

	async function save() {
		const ok = await saving.run(() => updateProject(project.id, { wish: text }));
		if (ok) {
			editing = false;
			changed();
		}
	}
</script>

{#if editing}
	<form
		onsubmit={(e) => {
			e.preventDefault();
			void save();
		}}
	>
		<label>
			{locale.t('wish.wishLabel')}
			<textarea rows="6" bind:value={text} data-testid="wish-edit"></textarea>
		</label>
		<p class="lead">{locale.t('wish.editWarning')}</p>
		<button type="submit" class="primary" disabled={saving.busy} data-testid="wish-save">
			{locale.t('list.save')}
		</button>
		<button type="button" onclick={() => (editing = false)}>{locale.t('list.cancel')}</button>
		{#if saving.error}<p role="alert">{saving.error}</p>{/if}
	</form>
{:else}
	<p class="lead" data-testid="wish-text">{project.wish}</p>
	<button onclick={start} data-testid="wish-edit-open">{locale.t('wish.edit')}</button>
{/if}
