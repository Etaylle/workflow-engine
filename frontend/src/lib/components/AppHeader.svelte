<script lang="ts">
	import { LOCALES } from '#lib/i18n/i18n.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { describeKey } from '#lib/ai/keyState.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import KeyPanel from './KeyPanel.svelte';
	import { activeChip } from '#lib/planning/steps.js';

	let { projectName, status }: { projectName?: string; status?: string } = $props();

	const CHIPS = ['wish', 'questions', 'requirements', 'workflow', 'review'] as const;
	const described = $derived(describeKey(engineKey.state));
	const active = $derived(status === undefined ? -1 : activeChip(status));
</script>

<header class="bar">
	<a class="brand" href="#projects">
		<span class="logo" aria-hidden="true">&#9679;&#8211;&#9679;</span>
		<strong>{locale.t('app.title')}</strong>
	</a>
	{#if projectName}<span class="crumb">/ {projectName}</span>{/if}
	{#if status !== undefined}
		<ol class="chips" data-testid="steps">
			{#each CHIPS as chip, index (chip)}
				<li
					class:current={index === active}
					class:done={active >= 0 && index < active}
					aria-current={index === active ? 'step' : undefined}
				>
					<span class="num" aria-hidden="true"
						>{active >= 0 && index < active ? '✓' : index + 1}</span
					>
					{locale.t(`nav.${chip}` as MessageKey)}
				</li>
			{/each}
		</ol>
	{/if}
	{#if engineKey.demo}
		<span class="demo" data-testid="demo-badge">{locale.t('app.demo')}</span>
	{/if}
	<button
		type="button"
		class="keybtn"
		class:missing={engineKey.state !== null && !engineKey.state.set}
		aria-expanded={engineKey.panelOpen}
		onclick={() => (engineKey.panelOpen ? engineKey.closePanel() : engineKey.openPanel())}
		data-testid="key-button"
	>
		{locale.t('key.button')}:
		{locale.t(described.key, described.params)}
	</button>
	<select
		class="lang"
		aria-label={locale.t('language.label')}
		value={locale.current}
		onchange={(e) => locale.set(e.currentTarget.value === 'de' ? 'de' : 'en')}
		data-testid="language"
	>
		{#each LOCALES as code (code)}
			<option value={code}>{locale.t(`language.${code}` as MessageKey)}</option>
		{/each}
	</select>
	{#if engineKey.panelOpen}<KeyPanel />{/if}
</header>

<style>
	.bar {
		position: relative;
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.75rem 1rem;
		padding: 0.75rem 1.25rem;
		background: var(--surface);
		border-bottom: 1px solid var(--line);
	}
	.brand {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		color: var(--ink);
		text-decoration: none;
		font-size: 1.15rem;
	}
	.logo {
		background: var(--blue);
		color: #fff;
		border-radius: 8px;
		padding: 0.3rem 0.45rem;
		font-size: 0.6rem;
		letter-spacing: -0.1em;
	}
	.crumb {
		color: var(--muted);
	}
	.chips {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		list-style: none;
		margin: 0 auto;
		padding: 0;
	}
	.chips li {
		border: 1px solid var(--line);
		border-radius: 999px;
		padding: 0.25rem 0.9rem;
		color: var(--muted);
	}
	.chips li {
		display: inline-flex;
		align-items: center;
		gap: 0.45rem;
	}
	.num {
		font-size: 0.8rem;
		min-width: 1.4rem;
		height: 1.4rem;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		background: var(--grey-soft);
	}
	.chips li.done {
		background: var(--green-soft);
		border-color: #c3e1b6;
		color: var(--green-ink);
	}
	.chips li.done .num {
		background: #c3e1b6;
	}
	.chips li.current .num {
		background: var(--blue);
		color: #fff;
	}
	.chips li.current {
		background: var(--blue-soft);
		border-color: var(--blue);
		color: #1b4f9f;
		font-weight: 600;
	}
	.keybtn {
		margin-left: auto;
		padding: 0.3rem 0.8rem;
	}
	.keybtn.missing {
		border-color: #e0a15c;
		background: var(--amber-soft);
		color: var(--amber-ink);
	}
	.demo {
		background: var(--amber-soft);
		color: var(--amber-ink);
		border: 1px solid #f0d3a8;
		border-radius: 999px;
		padding: 0.2rem 0.8rem;
		font-size: 0.85rem;
		font-weight: 600;
	}
</style>
