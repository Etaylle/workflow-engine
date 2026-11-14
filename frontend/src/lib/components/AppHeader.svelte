<script lang="ts">
	import { LOCALES } from '#lib/i18n/i18n.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { activeChip } from '#lib/planning/steps.js';

	let { projectName, status }: { projectName?: string; status?: string } = $props();

	const CHIPS = ['wish', 'questions', 'requirements', 'workflow', 'review'] as const;
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
				<li class:current={index === active} aria-current={index === active ? 'step' : undefined}>
					{index + 1}
					{locale.t(`nav.${chip}` as MessageKey)}
				</li>
			{/each}
		</ol>
	{/if}
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
</header>

<style>
	.bar {
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
	.chips li.current {
		background: var(--blue-soft);
		border-color: var(--blue);
		color: #1b4f9f;
		font-weight: 600;
	}
	.lang {
		margin-left: auto;
	}
</style>
