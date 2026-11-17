<script lang="ts">
	import {
		ApiError,
		checkAiConnection,
		choosePlanningProvider,
		deleteAiKey,
		setAiKey,
		type AiProviderId
	} from '#lib/api/client.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { keyWarning, typedKey } from '#lib/ai/keyState.js';
	import { DEFAULT_PRESET, PRESETS, presetOfUrl, type PresetId } from '#lib/ai/presets.js';
	import { onMount } from 'svelte';
	import { toasts } from '#lib/shell/toast.svelte.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	const saved = engineKey.keys?.openai;
	const startPreset = saved?.set ? presetOfUrl(saved.baseUrl) : DEFAULT_PRESET;

	let inputs = $state<Record<AiProviderId, string>>({ anthropic: '', openai: '' });
	let presetId = $state<PresetId>(startPreset.id);
	let baseUrl = $state(saved?.set ? (saved.baseUrl ?? '') : startPreset.baseUrl);
	let model = $state(saved?.set ? (saved.model ?? '') : startPreset.model);
	let busy = $state<AiProviderId | null>(null);
	let results = $state<Record<AiProviderId, { ok: boolean; text: string } | null>>({
		anthropic: null,
		openai: null
	});

	let firstInput = $state<HTMLInputElement>();

	onMount(() => firstInput?.focus());

	function close() {
		engineKey.closePanel();
		document.querySelector<HTMLElement>('[data-testid="key-button"]')?.focus();
	}

	const preset = $derived(PRESETS.find((p) => p.id === presetId) ?? DEFAULT_PRESET);
	const keys = $derived(engineKey.keys);
	const bothSet = $derived(keys !== null && keys.anthropic.set && keys.openai.set);

	function pick(id: PresetId) {
		presetId = id;
		const next = PRESETS.find((p) => p.id === id);
		if (next && next.id !== 'custom') {
			baseUrl = next.baseUrl;
			model = next.model;
		}
	}

	function body(provider: AiProviderId, key: string) {
		return provider === 'openai' ? { apiKey: key, baseUrl, model } : { apiKey: key };
	}

	async function act(provider: AiProviderId, done: MessageKey, call: () => Promise<unknown>) {
		busy = provider;
		results[provider] = null;
		try {
			await call();
			results[provider] = { ok: true, text: locale.t(done) };
			toasts.show(locale.t(done));
			inputs[provider] = '';
		} catch (e) {
			const detail = e instanceof ApiError ? e.message : locale.t('ai.unreachable');
			const kept = engineKey.keys?.[provider].set ? ` ${locale.t('key.kept')}` : '';
			results[provider] = { ok: false, text: locale.t('ai.failed', { detail }) + kept };
		} finally {
			busy = null;
			await engineKey.refresh();
		}
	}

	const save = (p: AiProviderId) => {
		const key = typedKey(inputs[p]);
		if (key) void act(p, 'key.saved', async () => engineKey.adopt(await setAiKey(p, body(p, key))));
	};
	const test = (p: AiProviderId) => {
		const key = typedKey(inputs[p]);
		void act(p, 'key.works', () =>
			checkAiConnection({
				provider: p,
				...(key ? { apiKey: key } : {}),
				...(p === 'openai' ? { baseUrl, model } : {})
			})
		);
	};
	const remove = (p: AiProviderId) => void act(p, 'key.removed', () => deleteAiKey(p));
	const choose = (p: AiProviderId) => void choosePlanningProvider(p).then(engineKey.adopt);
</script>

<svelte:window onkeydown={(e) => e.key === 'Escape' && close()} />

<div class="panel" role="dialog" aria-label={locale.t('key.title')} data-testid="key-panel">
	<div class="head">
		<h2>{locale.t('key.title')}</h2>
		<button class="quiet" onclick={close} data-testid="key-close">
			{locale.t('key.close')}
		</button>
	</div>
	<p class="hint" data-testid="key-note">
		{locale.t(keys?.saved ? 'key.noteSaved' : 'key.note')}
	</p>

	<section class="slot">
		<h3>{locale.t('key.anthropic')}</h3>
		<label>
			{locale.t('key.field')}
			<input
				type="password"
				autocomplete="off"
				bind:this={firstInput}
				bind:value={inputs.anthropic}
				data-testid="key-input"
			/>
		</label>
		{#if keyWarning('anthropic', inputs.anthropic)}
			<p class="warn" role="status">{locale.t(keyWarning('anthropic', inputs.anthropic)!)}</p>
		{/if}
		<div class="row">
			<button
				class="primary"
				disabled={busy !== null || typedKey(inputs.anthropic) === null}
				onclick={() => save('anthropic')}
				data-testid="key-save"
			>
				{locale.t('key.save')}
			</button>
			<button
				disabled={busy !== null || (typedKey(inputs.anthropic) === null && !keys?.anthropic.set)}
				onclick={() => test('anthropic')}
				data-testid="key-test"
			>
				{busy === 'anthropic' ? locale.t('wish.testing') : locale.t('wish.test')}
			</button>
			<button
				class="danger"
				disabled={busy !== null || !keys?.anthropic.set}
				onclick={() => remove('anthropic')}
				data-testid="key-remove"
			>
				{locale.t('key.remove')}
			</button>
		</div>
		{#if results.anthropic}
			<p
				class:ok={results.anthropic.ok}
				role={results.anthropic.ok ? 'status' : 'alert'}
				data-testid="key-panel-result"
			>
				{results.anthropic.text}
			</p>
		{/if}
	</section>

	<section class="slot">
		<h3>{locale.t('key.openai')}</h3>
		<label>
			{locale.t('key.preset')}
			<select
				value={presetId}
				onchange={(e) => pick(e.currentTarget.value as PresetId)}
				data-testid="key-preset"
			>
				{#each PRESETS as option (option.id)}
					<option value={option.id}>{locale.t(option.label)}</option>
				{/each}
			</select>
		</label>
		<p class="hint preset-hint">{locale.t(preset.hint)}</p>
		<label>
			{preset.name
				? locale.t('key.fieldFor', { provider: preset.name })
				: locale.t('key.fieldFor', { provider: locale.t('key.openai') })}
			<input
				type="password"
				autocomplete="off"
				bind:value={inputs.openai}
				data-testid="key-input-openai"
			/>
		</label>
		{#if keyWarning('openai', inputs.openai)}
			<p class="warn" role="status">{locale.t(keyWarning('openai', inputs.openai)!)}</p>
		{/if}
		<label>
			{locale.t('key.baseUrl')}
			<input type="text" bind:value={baseUrl} data-testid="key-baseurl" />
		</label>
		<label>
			{locale.t('key.model')}
			<input type="text" bind:value={model} data-testid="key-model" />
		</label>
		<div class="row">
			<button
				class="primary"
				disabled={busy !== null || typedKey(inputs.openai) === null}
				onclick={() => save('openai')}
				data-testid="key-save-openai"
			>
				{locale.t('key.save')}
			</button>
			<button
				disabled={busy !== null || (typedKey(inputs.openai) === null && !keys?.openai.set)}
				onclick={() => test('openai')}
				data-testid="key-test-openai"
			>
				{busy === 'openai' ? locale.t('wish.testing') : locale.t('wish.test')}
			</button>
			<button
				class="danger"
				disabled={busy !== null || !keys?.openai.set}
				onclick={() => remove('openai')}
				data-testid="key-remove-openai"
			>
				{locale.t('key.remove')}
			</button>
		</div>
		{#if results.openai}
			<p
				class:ok={results.openai.ok}
				role={results.openai.ok ? 'status' : 'alert'}
				data-testid="key-panel-result-openai"
			>
				{results.openai.text}
			</p>
		{/if}
	</section>

	{#if bothSet}
		<fieldset class="choose" data-testid="key-choose">
			<legend>{locale.t('key.choose')}</legend>
			{#each ['anthropic', 'openai'] as const as provider (provider)}
				<label class="pick">
					<input
						type="radio"
						name="planning-provider"
						checked={keys?.planning?.provider === provider}
						onchange={() => choose(provider)}
						data-testid={`choose-${provider}`}
					/>
					{engineKey.nameOf(locale.t('key.nameAnthropic'), provider)}
				</label>
			{/each}
		</fieldset>
	{/if}
	<p class="hint" data-testid="key-planning">
		{#if keys?.planning}
			{locale.t('key.planningWith')}
			{engineKey.nameOf(locale.t('key.nameAnthropic'), keys.planning.provider)} · {keys.planning
				.model}
		{:else}
			{locale.t('key.planningNone')}
		{/if}
	</p>
</div>

<style>
	.panel {
		position: absolute;
		right: var(--gutter);
		top: calc(100% + var(--space-2));
		z-index: 20;
		width: min(30rem, calc(100vw - 2 * var(--gutter)));
		max-height: calc(100vh - 6rem);
		overflow-y: auto;
		background: var(--surface);
		border: 1px solid var(--line);
		border-radius: var(--radius);
		box-shadow: var(--shadow-pop);
		padding: var(--space-4) var(--space-5) var(--space-5);
	}
	.head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: var(--space-4);
	}
	h2 {
		font-size: var(--text-lg);
		margin: 0;
	}
	h3 {
		font-size: var(--text-base);
		margin: 0 0 var(--space-2);
	}
	.slot {
		border-top: 1px solid var(--line);
		padding-top: var(--space-4);
		margin: var(--space-4) 0 0;
	}
	.slot label {
		margin-bottom: var(--space-3);
	}
	.preset-hint {
		margin: 0 0 var(--space-3);
	}
	.warn {
		margin: 0 0 var(--space-3);
		color: var(--amber-ink);
		background: var(--amber-soft);
		border-radius: var(--radius-sm);
		padding: var(--space-2) var(--space-3);
		font-size: var(--text-sm);
	}
	.row {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-2);
	}
	.ok {
		color: var(--green-ink);
		margin: var(--space-3) 0 0;
	}
	.choose {
		margin-top: var(--space-4);
	}
	.pick {
		display: inline-block;
		margin: 0 var(--space-4) 0 0;
		font-weight: 400;
	}
</style>
