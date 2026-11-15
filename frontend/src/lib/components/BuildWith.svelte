<script lang="ts">
	import { getBuilders, setClaudeAuth, type Builder, type Builders } from '#lib/api/execution.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { actionFor, builderIds, otherAuthMode } from '#lib/execution/builders.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	/** The two builders as cards: readiness, model, and for each missing item an action. */
	let {
		harness,
		builders,
		update,
		choose: pick
	}: {
		harness: string;
		builders: Builders;
		update: (fresh: Builders | null) => void;
		choose: (id: Builder['id']) => Promise<void>;
	} = $props();

	const acting = createAction();

	async function choose(id: Builder['id']) {
		if (harness === id) return;
		await acting.run(() => pick(id));
	}

	async function auth(builder: Builder) {
		if (await acting.run(() => setClaudeAuth(otherAuthMode(builder)))) update(await getBuilders());
	}

	async function check() {
		await acting.run(async () => update(await getBuilders(true)));
	}

	const switchesAuth = (id: string, code: string) =>
		id === 'claude_code' && (code === 'no_anthropic_key' || code === 'not_signed_in');
</script>

<div class="card build" data-testid="build-with">
	<h3>{locale.t('build.heading')}</h3>
	<p class="hint">{locale.t('build.lead')}</p>
	<div class="cards" role="radiogroup" aria-label={locale.t('build.heading')}>
		{#each builderIds as id (id)}
			{@const builder = builders.builders.find((b) => b.id === id)}
			{#if builder}
				{@const chosen = harness === id}
				<div class="option" class:chosen data-testid={`builder-${id}`}>
					<button
						type="button"
						class="pick"
						role="radio"
						aria-checked={chosen}
						onclick={() => choose(id)}
						disabled={acting.busy}
						data-testid={`pick-${id}`}
					>
						<strong>{locale.t(`build.${id}` as MessageKey)}</strong>
						<span class="state" class:ok={builder.ready} data-testid={`state-${id}`}>
							{builder.ready ? locale.t('build.ready') : locale.t('build.notReady')}
						</span>
						{#if builder.model}
							<span class="hint">{locale.t('build.model', { model: builder.model })}</span>
						{/if}
					</button>
					{#each builder.missing as item (item.code)}
						<div class="missing" data-testid={`missing-${item.code}`}>
							<p>
								{locale.has(`build.code.${item.code}`)
									? locale.t(`build.code.${item.code}` as MessageKey)
									: item.message}
							</p>
							{#if item.instructionKey && locale.has(item.instructionKey)}
								<p class="hint">{locale.t(item.instructionKey as MessageKey)}</p>
							{/if}
							<div class="line">
								{#if actionFor(item.code) === 'open_key_panel'}
									<button type="button" onclick={() => engineKey.openPanel()}>
										{locale.t(id === 'gemini_cli' ? 'build.enterGeminiKey' : 'build.enterKey')}
									</button>
								{/if}
								{#if switchesAuth(id, item.code)}
									<button type="button" onclick={() => auth(builder)} disabled={acting.busy}>
										{locale.t(
											otherAuthMode(builder) === 'machine_sign_in'
												? 'build.useSignIn'
												: 'build.useKey'
										)}
									</button>
								{/if}
							</div>
						</div>
					{/each}
				</div>
			{/if}
		{/each}
	</div>
	<div class="line">
		<button
			type="button"
			class="quiet"
			onclick={check}
			disabled={acting.busy}
			data-testid="check-again"
		>
			{locale.t('build.checkAgain')}
		</button>
		{#if builders.demo}<span class="hint">{locale.t('build.demo')}</span>{/if}
	</div>
	{#if acting.error}<p role="alert">{acting.error}</p>{/if}
</div>

<style>
	.cards {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
		gap: var(--space-3);
	}
	.option {
		border: 1px solid var(--line);
		border-radius: var(--radius-sm);
		padding: var(--space-3) var(--space-4);
	}
	.option.chosen {
		border-color: var(--blue);
		background: var(--blue-soft);
	}
	.pick {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
		gap: var(--space-1);
		width: 100%;
		background: transparent;
		border: 0;
		padding: 0;
		text-align: left;
		color: inherit;
	}
	.line {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: var(--space-2) var(--space-3);
		margin: var(--space-2) 0 0;
	}
	.missing {
		margin-top: var(--space-3);
		border-top: 1px dashed var(--line);
		padding-top: var(--space-2);
	}
	.missing p {
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
</style>
