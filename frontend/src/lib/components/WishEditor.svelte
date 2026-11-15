<script lang="ts">
	import { updateProject, type Project } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { toasts } from '#lib/shell/toast.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	let { project, changed }: { project: Project; changed: () => Promise<void> } = $props();

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
			await changed();
			toasts.show(locale.t('toast.wishSaved'));
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
		<p class="hint">{locale.t('wish.editWarning')}</p>
		<div class="row">
			<button type="submit" disabled={saving.busy} data-testid="wish-save">
				{locale.t('list.save')}
			</button>
			<button type="button" class="quiet" onclick={() => (editing = false)}>
				{locale.t('list.cancel')}
			</button>
		</div>
		{#if saving.error}<p role="alert">{saving.error}</p>{/if}
	</form>
{:else}
	<p class="wish-text" data-testid="wish-text">{project.wish}</p>
	<div class="optional">
		<span class="optional-label">{locale.t('step.optional')}</span>
		<button class="quiet" onclick={start} data-testid="wish-edit-open"
			>{locale.t('wish.edit')}</button
		>
	</div>
{/if}

<style>
	.wish-text {
		white-space: pre-wrap;
		font-size: var(--text-lg);
		margin: 0;
		padding: var(--space-4);
		background: var(--sunken);
		border-radius: var(--radius-sm);
		border: 1px solid var(--line);
	}
	.row {
		display: flex;
		gap: var(--space-2);
	}
</style>
