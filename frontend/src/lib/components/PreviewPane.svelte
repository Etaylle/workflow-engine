<script lang="ts">
	import type { Preview, RunEvent } from '#lib/api/client.js';
	import ConsolePane from '#lib/components/ConsolePane.svelte';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { appUrl, minutesLeft, previewActive } from '#lib/run/preview.js';

	/** The generated app tried out: its state, the link to open it and what it prints. */
	let {
		preview,
		lines,
		hasOlder,
		loadingOlder,
		loadOlder,
		stop,
		stopping
	}: {
		preview: Preview;
		lines: RunEvent[];
		hasOlder: boolean;
		loadingOlder: boolean;
		loadOlder: () => void;
		stop: () => void;
		stopping: boolean;
	} = $props();

	let now = $state(Date.now());
	$effect(() => {
		if (preview.status !== 'running') return;
		const timer = setInterval(() => (now = Date.now()), 15_000);
		return () => clearInterval(timer);
	});

	const url = $derived(appUrl(preview, location.hostname));
	const left = $derived(minutesLeft(preview, now));
</script>

<div class="preview" data-testid="preview">
	<div class="head">
		<span class="pill" data-testid="preview-status" data-status={preview.status}>
			{locale.t(`preview.status.${preview.status}` as MessageKey)}
		</span>
		{#if preview.message && preview.status !== 'running'}
			<span class="note" data-testid="preview-message">{preview.message}</span>
		{/if}
		{#if url}
			<a class="button primary" href={url} target="_blank" rel="noopener" data-testid="open-app">
				{locale.t('preview.open')}
			</a>
		{/if}
		{#if left !== null}
			<span class="note">{locale.t('preview.left', { minutes: String(left) })}</span>
		{/if}
		{#if previewActive(preview)}
			<button class="quiet" onclick={stop} disabled={stopping} data-testid="stop-preview">
				{locale.t('preview.stop')}
			</button>
		{/if}
	</div>
	<ConsolePane
		{lines}
		testid="app-console"
		label={locale.t('run.tab.app')}
		empty={locale.t('preview.empty')}
		{hasOlder}
		{loadingOlder}
		{loadOlder}
	/>
</div>

<style>
	.preview {
		display: flex;
		flex-direction: column;
		flex: 1 1 auto;
		min-height: 0;
		gap: var(--space-3);
	}
	.head {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: var(--space-3);
	}
	.note {
		color: var(--muted);
		font-size: var(--text-sm);
	}
	.button {
		text-decoration: none;
	}
</style>
