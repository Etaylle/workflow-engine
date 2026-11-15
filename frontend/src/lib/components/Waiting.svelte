<script lang="ts">
	import { onMount } from 'svelte';
	import { locale } from '#lib/i18n/state.svelte.js';

	/** What the AI is doing right now, already translated. */
	let { what }: { what: string } = $props();

	let seconds = $state(0);

	onMount(() => {
		const started = Date.now();
		const timer = setInterval(() => (seconds = Math.floor((Date.now() - started) / 1000)), 1000);
		return () => clearInterval(timer);
	});
</script>

<div class="waiting" role="status" aria-live="polite" data-testid="waiting">
	<span class="spinner" aria-hidden="true"></span>
	<div>
		<strong>{what}</strong>
		<div class="hint">{locale.t('wait.hint')}</div>
	</div>
	<span class="elapsed">{locale.t('wait.elapsed', { seconds: String(seconds) })}</span>
</div>

<style>
	.waiting {
		display: flex;
		align-items: center;
		gap: 0.9rem;
		padding: 0.9rem 1.1rem;
		margin-top: 1rem;
		border: 1px solid var(--blue-line);
		border-radius: var(--radius);
		background: var(--blue-soft);
		color: var(--blue-ink);
	}
	.hint {
		font-size: 0.9rem;
		opacity: 0.85;
	}
	.elapsed {
		margin-left: auto;
		font-variant-numeric: tabular-nums;
		font-size: 0.9rem;
	}
</style>
