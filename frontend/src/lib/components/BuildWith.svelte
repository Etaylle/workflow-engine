<script lang="ts">
	import { updateProject, type Project } from '#lib/api/client.js';
	import { setClaudeAuth, type ClaudeAuthMode } from '#lib/api/execution.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { actionFor, claudeAuthOptions, readinessOf } from '#lib/execution/builders.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import type { Prerequisites } from '#lib/api/execution.js';

	/** The builder and its authorisation, with what is missing and an action for each missing thing. */
	let {
		project,
		prerequisites,
		reload,
		changed
	}: {
		project: Project;
		prerequisites: Prerequisites;
		reload: () => Promise<void>;
		changed: () => Promise<void>;
	} = $props();

	const acting = createAction();
	const builders = ['claude_code', 'gemini_cli'] as const;

	const label = (code: string) =>
		code === 'anthropic_key' || code === 'machine_sign_in' || code === 'gemini_key'
			? locale.t(`build.missing.${code}` as MessageKey)
			: code;

	async function chooseBuilder(id: (typeof builders)[number]) {
		if (project.agentHarness === id) return;
		if (await acting.run(() => updateProject(project.id, { agentHarness: id }))) {
			await changed();
			await reload();
		}
	}

	async function chooseAuth(mode: ClaudeAuthMode) {
		if (await acting.run(() => setClaudeAuth(mode))) await reload();
	}
</script>

<div class="card build" data-testid="build-with">
	<h3>{locale.t('build.heading')}</h3>
	<p class="hint">{locale.t('build.lead')}</p>
	<ul class="builders">
		{#each builders as id (id)}
			{@const readiness = readinessOf(prerequisites, id)}
			{@const chosen = project.agentHarness === id}
			{#if readiness}
				<li class:chosen data-testid={`builder-${id}`}>
					<div class="line">
						<strong>{locale.t(`build.${id}` as MessageKey)}</strong>
						{#if chosen}<span class="badge">{locale.t('build.chosen')}</span>{/if}
						<span class="state" class:ok={readiness.ready}>
							{readiness.ready
								? locale.t('build.ready')
								: locale.t('build.missing', { what: readiness.missing.map(label).join(', ') })}
						</span>
						{#if !chosen}
							<button type="button" onclick={() => chooseBuilder(id)} disabled={acting.busy}>
								{locale.t(id === 'gemini_cli' ? 'build.useGemini' : 'build.useClaude')}
							</button>
						{/if}
					</div>
					{#if readiness.details}<p class="hint">{readiness.details}</p>{/if}
					{#if chosen && id === 'claude_code'}
						<fieldset>
							<legend>{locale.t('build.authHeading')}</legend>
							{#each claudeAuthOptions(prerequisites) as option (option.mode)}
								<div class="line" data-testid={`auth-${option.mode}`}>
									<span>{locale.t(`build.auth.${option.mode}` as MessageKey)}</span>
									{#if option.selected}<span class="badge">{locale.t('build.chosen')}</span>{/if}
									<span class="state" class:ok={option.ready}>
										{option.ready
											? locale.t('build.ready')
											: locale.t('build.missing', { what: label(option.mode) })}
									</span>
									{#if option.mode === 'anthropic_key'}
										{#if !option.ready}
											<button type="button" onclick={() => engineKey.openPanel()}>
												{locale.t('build.enterKey')}
											</button>
										{/if}
										{#if !option.selected}
											<button type="button" onclick={() => chooseAuth(option.mode)}>
												{locale.t('build.auth.anthropic_key')}
											</button>
										{/if}
									{:else if !option.selected || !option.ready}
										<button
											type="button"
											onclick={() => chooseAuth(option.mode)}
											disabled={acting.busy}
										>
											{locale.t('build.useSignIn')}
										</button>
									{/if}
								</div>
								{#if option.mode === 'machine_sign_in' && !option.ready}
									<p class="hint">{locale.t('build.signInHint')}</p>
								{/if}
							{/each}
						</fieldset>
					{/if}
					{#if chosen && id === 'gemini_cli' && readiness.missing.some((c) => actionFor(c).kind === 'open_key_panel')}
						<button type="button" onclick={() => engineKey.openPanel()}>
							{locale.t('build.enterGeminiKey')}
						</button>
					{/if}
				</li>
			{/if}
		{/each}
	</ul>
	{#if acting.error}<p role="alert">{acting.error}</p>{/if}
</div>

<style>
	.builders {
		list-style: none;
		margin: 0;
		padding: 0;
		display: grid;
		gap: var(--space-3);
	}
	.builders li {
		border: 1px solid var(--line);
		border-radius: var(--radius-sm);
		padding: var(--space-3) var(--space-4);
	}
	.builders li.chosen {
		border-color: var(--blue);
		background: var(--blue-soft);
	}
	.line {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: var(--space-2) var(--space-3);
		margin: var(--space-1) 0;
	}
	.state {
		color: var(--amber-ink);
		font-size: var(--text-sm);
		font-weight: 500;
	}
	.state.ok {
		color: var(--green-ink);
	}
	fieldset {
		margin-top: var(--space-3);
		background: transparent;
	}
</style>
