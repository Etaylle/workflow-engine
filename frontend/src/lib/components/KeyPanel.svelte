<script lang="ts">
	import { ApiError, checkAiConnection, deleteAiKey, setAiKey } from '#lib/api/client.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { typedKey } from '#lib/ai/keyState.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	let input = $state('');
	let busy = $state(false);
	let result = $state<{ ok: boolean; text: string } | null>(null);
	const key = $derived(typedKey(input));

	async function act(done: MessageKey, call: (typed: string | null) => Promise<unknown>) {
		busy = true;
		result = null;
		try {
			await call(key);
			result = { ok: true, text: locale.t(done) };
			input = '';
		} catch (e) {
			const detail = e instanceof ApiError ? e.message : locale.t('ai.unreachable');
			result = { ok: false, text: locale.t('ai.failed', { detail }) };
		} finally {
			busy = false;
			await engineKey.refresh();
		}
	}
</script>

<div class="panel" role="dialog" aria-label={locale.t('key.title')} data-testid="key-panel">
	<h2>{locale.t('key.title')}</h2>
	<p class="note">{locale.t('key.note')}</p>
	<label>
		{locale.t('key.field')}
		<input type="password" autocomplete="off" bind:value={input} data-testid="key-input" />
	</label>
	<div class="row">
		<button
			class="primary"
			disabled={busy || key === null}
			onclick={() => act('key.saved', (k) => setAiKey(k ?? ''))}
			data-testid="key-save"
		>
			{locale.t('key.save')}
		</button>
		<button
			disabled={busy || key === null}
			onclick={() => act('key.works', (k) => checkAiConnection(k ?? ''))}
			data-testid="key-test"
		>
			{busy ? locale.t('wish.testing') : locale.t('wish.test')}
		</button>
		<button
			class="danger"
			disabled={busy || !engineKey.state?.set}
			onclick={() => act('key.removed', () => deleteAiKey())}
			data-testid="key-remove"
		>
			{locale.t('key.remove')}
		</button>
		<button onclick={() => engineKey.closePanel()} data-testid="key-close">
			{locale.t('key.close')}
		</button>
	</div>
	{#if result}
		<p class:ok={result.ok} role={result.ok ? 'status' : 'alert'} data-testid="key-panel-result">
			{result.text}
		</p>
	{/if}
</div>

<style>
	.panel {
		position: absolute;
		right: 1.25rem;
		top: calc(100% + 0.4rem);
		z-index: 20;
		width: min(26rem, calc(100vw - 2.5rem));
		background: var(--surface);
		border: 1px solid var(--line);
		border-radius: var(--radius);
		box-shadow: 0 8px 28px rgba(29, 29, 27, 0.18);
		padding: 1rem 1.25rem 1.1rem;
	}
	h2 {
		font-size: 1.05rem;
		margin: 0 0 0.3rem;
	}
	.note {
		margin: 0 0 0.8rem;
		color: var(--muted);
		font-size: 0.9rem;
	}
	.row {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
	}
	.ok {
		color: var(--green-ink);
		margin: 0.8rem 0 0;
	}
</style>
