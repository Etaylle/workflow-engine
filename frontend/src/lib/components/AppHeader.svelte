<script lang="ts">
	import { LOCALES } from '#lib/i18n/i18n.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { describeKey } from '#lib/ai/keyState.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import KeyPanel from './KeyPanel.svelte';
	import { nextActionKey, reach, STEPS, type StepId } from '#lib/planning/steps.js';
	import { stepHash } from '#lib/shell/route.js';

	let {
		projectId,
		projectName,
		status,
		step
	}: {
		projectId?: string;
		projectName?: string;
		status?: string;
		step?: StepId | undefined;
	} = $props();

	const nextKey = $derived(status === undefined ? null : nextActionKey(status));
	const described = $derived(
		describeKey(engineKey.keys, (p) => engineKey.nameOf(locale.t('key.nameAnthropic'), p))
	);
	const reached = $derived(status === undefined ? -1 : reach(status));
</script>

<header class="bar">
	<a class="brand" href="#projects">
		<span class="logo" aria-hidden="true">&#9679;&#8211;&#9679;</span>
		<strong>{locale.t('app.title')}</strong>
	</a>
	{#if projectName}<span class="crumb">/ {projectName}</span>{/if}
	{#if status !== undefined && projectId}
		<nav class="stepper" aria-label={locale.t('stepper.label')}>
			<ol class="chips" data-testid="steps">
				{#each STEPS as chip, index (chip)}
					<li
						class:current={chip === step}
						class:done={index < reached}
						class:locked={index > reached}
					>
						{#if index <= reached}
							<a
								class="chip"
								href={stepHash(projectId, chip)}
								aria-current={chip === step ? 'step' : undefined}
								data-testid={`chip-${chip}`}
							>
								<span class="num" aria-hidden="true">{index < reached ? '✓' : index + 1}</span>
								{locale.t(`nav.${chip}` as MessageKey)}
							</a>
						{:else}
							<span class="chip" aria-disabled="true" data-testid={`chip-${chip}`}>
								<span class="num" aria-hidden="true">{index + 1}</span>
								{locale.t(`nav.${chip}` as MessageKey)}
							</span>
						{/if}
					</li>
				{/each}
			</ol>
			{#if nextKey}
				<p class="next" data-testid="next-action">
					{locale.t('stepper.next', { action: locale.t(nextKey as MessageKey) })}
				</p>
			{/if}
		</nav>
	{/if}
	{#if engineKey.demo}
		<span class="demo" data-testid="demo-badge">{locale.t('app.demo')}</span>
	{/if}
	<button
		type="button"
		class="keybtn"
		class:missing={engineKey.keys !== null && !engineKey.anySet}
		aria-expanded={engineKey.panelOpen}
		onclick={() => (engineKey.panelOpen ? engineKey.closePanel() : engineKey.openPanel())}
		data-testid="key-button"
	>
		{locale.t('key.keyButton')}:
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
		position: sticky;
		top: 0;
		z-index: 10;
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.5rem 0.75rem;
		padding: var(--space-3) var(--gutter);
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
		color: var(--on-blue);
		border-radius: 8px;
		padding: 0.3rem 0.45rem;
		font-size: 0.6rem;
		letter-spacing: -0.1em;
	}
	.crumb {
		color: var(--muted);
	}
	.stepper {
		margin: 0 auto;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: var(--space-1);
	}
	.next {
		margin: 0;
		font-size: var(--text-sm);
		font-weight: 600;
		color: var(--blue-ink);
	}
	.chips {
		display: flex;
		flex-wrap: wrap;
		justify-content: center;
		gap: var(--space-2);
		list-style: none;
		margin: 0;
		padding: 0;
	}
	.chips li {
		border: 1px solid var(--line);
		border-radius: 999px;
		padding: 0.2rem 0.65rem;
		font-size: var(--text-sm);
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
	.chip {
		display: inline-flex;
		align-items: center;
		gap: 0.45rem;
		min-height: 2rem;
		color: inherit;
		text-decoration: none;
	}
	span.chip {
		cursor: not-allowed;
	}
	.chips li.locked {
		opacity: 0.6;
	}
	.chips li.done {
		background: var(--green-soft);
		border-color: var(--green-line);
		color: var(--green-ink);
	}
	.chips li.done .num {
		background: var(--green-line);
	}
	.chips li.current .num {
		background: var(--blue);
		color: var(--on-blue);
	}
	.chips li.current {
		background: var(--blue-soft);
		border-color: var(--blue);
		color: var(--blue-ink);
		font-weight: 600;
	}
	.keybtn {
		margin-left: auto;
		min-height: 2.25rem;
		padding: 0 var(--space-3);
	}
	.keybtn.missing {
		border-color: var(--amber-line);
		background: var(--amber-soft);
		color: var(--amber-ink);
	}
	.lang {
		width: auto;
		min-height: 2.25rem;
	}
	.demo {
		background: var(--amber-soft);
		color: var(--amber-ink);
		border: 1px solid var(--amber-line);
		border-radius: 999px;
		padding: 0.2rem 0.8rem;
		font-size: var(--text-xs);
		font-weight: 600;
	}
	@media (max-width: 52rem) {
		.bar {
			position: static;
		}
	}
</style>
