<script lang="ts">
	import { tick } from 'svelte';
	import type { RunEvent } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	/**
	 * A monospace log that follows its newest line. "Pause scrolling" keeps the view where it is
	 * while lines go on arriving; older lines than the buffer holds come on demand.
	 */
	let {
		lines,
		testid,
		label,
		empty,
		hasOlder = false,
		loadingOlder = false,
		loadOlder = () => {}
	}: {
		lines: RunEvent[];
		testid: string;
		label: string;
		empty: string;
		hasOlder?: boolean;
		loadingOlder?: boolean;
		loadOlder?: () => void;
	} = $props();

	let paused = $state(false);
	let box = $state<HTMLElement>();

	$effect(() => {
		void lines.length;
		if (paused || !box) return;
		void tick().then(() => {
			if (box) box.scrollTop = box.scrollHeight;
		});
	});
</script>

<div class="pane">
	<div class="bar">
		<span class="count">{locale.t('console.lines', { count: String(lines.length) })}</span>
		<button
			type="button"
			class="quiet"
			aria-pressed={paused}
			onclick={() => (paused = !paused)}
			data-testid={`${testid}-pause`}
		>
			{paused ? locale.t('console.follow') : locale.t('console.pause')}
		</button>
	</div>
	<!-- svelte-ignore a11y_no_noninteractive_tabindex (a scrolling log must be reachable by keyboard) -->
	<div
		class="log"
		bind:this={box}
		role="log"
		aria-label={label}
		aria-live={paused ? 'off' : 'polite'}
		tabindex="0"
		data-testid={testid}
	>
		{#if hasOlder}
			<button
				type="button"
				class="quiet older"
				onclick={loadOlder}
				disabled={loadingOlder}
				data-testid={`${testid}-older`}
			>
				{locale.t('console.older')}
			</button>
		{/if}
		{#each lines as line (line.seq)}
			<div class="line">{line.text}</div>
		{:else}
			<p class="empty">{empty}</p>
		{/each}
	</div>
</div>

<style>
	.pane {
		display: flex;
		flex-direction: column;
		flex: 1 1 auto;
		min-height: 0;
		gap: var(--space-2);
	}
	.bar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: var(--space-3);
	}
	.count {
		color: var(--muted);
		font-size: var(--text-xs);
	}
	.log {
		flex: 1 1 auto;
		min-height: 0;
		overflow: auto;
		background: var(--sunken);
		border: 1px solid var(--line);
		border-radius: var(--radius-sm);
		padding: var(--space-3);
		font-family: ui-monospace, 'SF Mono', Menlo, Consolas, monospace;
		font-size: var(--text-xs);
		line-height: 1.5;
	}
	.line {
		white-space: pre-wrap;
		overflow-wrap: anywhere;
	}
	.older {
		display: block;
		margin: 0 auto var(--space-3);
	}
	.empty {
		margin: 0;
		color: var(--muted);
		font-family: system-ui, sans-serif;
	}
</style>
