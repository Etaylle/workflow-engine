<script lang="ts">
	import { checkAiConnection, resumeRun, stopRun, type Run } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import ConfirmDialog from '#lib/components/ConfirmDialog.svelte';
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
		<button class="danger" onclick={() => (confirming = true)} data-testid="stop-run">
			{locale.t('run.stop')}
		</button>
		{#if stopping.error && !confirming}<p role="alert" data-testid="stop-error">
				{stopping.error}
			</p>{/if}
	</div>
	<ConfirmDialog
		open={confirming}
		title={locale.t('run.stop.title')}
		text={locale.t('run.stop.confirmText')}
		confirmLabel={locale.t('run.stop.confirm')}
		busy={stopping.busy}
		error={stopping.error}
		testid="stop-confirm"
		onconfirm={stop}
		oncancel={() => (confirming = false)}
	/>
{/if}

<style>
	.controls {
		margin: 0.75rem 0;
	}
</style>
