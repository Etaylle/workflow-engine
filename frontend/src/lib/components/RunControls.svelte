<script lang="ts">
	import { checkAiConnection, resumeRun, stopRun, type Run } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { atCostLimit, canResume, canStop, limitHasRoom, suggestedLimit } from '#lib/run/run.js';

	let { run, updated }: { run: Run; updated: (run: Run) => void } = $props();

	let confirming = $state(false);
	let key = $state('');
	let typedLimit = $state<number | null>(null);
	const limit = $derived(typedLimit ?? suggestedLimit(run));
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
			if (key.trim()) await checkAiConnection({ provider: 'anthropic', apiKey: key.trim() });
			key = '';
			const raised = atCostLimit(run) ? Number(limit) : undefined;
			updated(await resumeRun(run.id, raised));
		});
	}
</script>

{#if canResume(run)}
	<div class="controls" data-testid="run-resume">
		{#if run.status === 'interrupted'}
			<p class="notice">
				{locale.t('run.resumeCount', { count: String(run.resumeCount) })}
			</p>
		{/if}
		{#if atCostLimit(run)}
			<label>
				{locale.t('run.raiseLimit')}
				<input
					type="number"
					min="1"
					step="1"
					value={limit}
					oninput={(event) => (typedLimit = event.currentTarget.valueAsNumber)}
					data-testid="raise-limit"
				/>
			</label>
		{/if}
		<label>
			{locale.t('run.resumeKey')}
			<input type="password" autocomplete="off" bind:value={key} data-testid="resume-key" />
		</label>
		<button
			class="primary"
			onclick={resume}
			disabled={resuming.busy || (atCostLimit(run) && !limitHasRoom(run, Number(limit)))}
			data-testid="resume-run"
		>
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
