<script lang="ts">
	import { checkAiConnection, resumeRun, stopRun, type Run } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { canResume, canStop } from '#lib/run/run.js';

	let { run, updated }: { run: Run; updated: (run: Run) => void } = $props();

	let confirming = $state(false);
	let key = $state('');
	const stopping = createAction();
	const resuming = createAction();

	async function stop() {
		await stopping.run(async () => {
			updated(await stopRun(run.id));
			confirming = false;
		});
	}

	async function resume() {
		await resuming.run(async () => {
			if (key.trim()) await checkAiConnection(key.trim());
			key = '';
			updated(await resumeRun(run.id));
		});
	}
</script>

{#if canResume(run)}
	<div class="controls" data-testid="run-resume">
		<p class="notice">
			{locale.t('run.resumeCount', { count: String(run.resumeCount) })}
		</p>
		<label>
			{locale.t('run.resumeKey')}
			<input type="password" autocomplete="off" bind:value={key} data-testid="resume-key" />
		</label>
		<button class="primary" onclick={resume} disabled={resuming.busy} data-testid="resume-run">
			{locale.t('run.resume')}
		</button>
		{#if resuming.error}<p role="alert" data-testid="resume-error">{resuming.error}</p>{/if}
	</div>
{/if}

{#if canStop(run)}
	<div class="controls">
		{#if !confirming}
			<button onclick={() => (confirming = true)} data-testid="stop-run">
				{locale.t('run.stop')}
			</button>
		{:else}
			<div role="alertdialog" aria-labelledby="stop-text" data-testid="stop-confirm">
				<p id="stop-text">{locale.t('run.stop.confirmText')}</p>
				<button
					class="danger"
					onclick={stop}
					disabled={stopping.busy}
					data-testid="stop-confirm-yes"
				>
					{locale.t('run.stop.confirm')}
				</button>
				<button onclick={() => (confirming = false)} data-testid="stop-confirm-no">
					{locale.t('run.stop.cancel')}
				</button>
			</div>
		{/if}
		{#if stopping.error}<p role="alert" data-testid="stop-error">{stopping.error}</p>{/if}
	</div>
{/if}

<style>
	.controls {
		margin: 0.75rem 0;
	}
	.danger {
		background: #a12121;
		color: #fff;
		border: none;
		border-radius: 8px;
		padding: 0.45rem 1rem;
	}
</style>
