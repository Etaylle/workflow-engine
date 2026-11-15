<script lang="ts">
	import { ApiError, checkAiConnection, createProject, type Project } from '#lib/api/client.js';
	import { describeError } from '#lib/api/problem.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { describeKey } from '#lib/ai/keyState.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	const MAX_WISH = 4000;

	let { created }: { created: (project: Project) => void } = $props();

	let name = $state('');
	let wish = $state('');
	let apiKey = $state('');
	let busy = $state(false);
	let error = $state<string | null>(null);
	let wishError = $state<string | null>(null);
	let wishField = $state<HTMLTextAreaElement>();
	const held = $derived(
		describeKey(engineKey.keys, (p) => engineKey.nameOf(locale.t('key.nameAnthropic'), p))
	);
	let keyCheck = $state<{ state: 'idle' | 'busy' | 'ok' | 'failed'; detail: string }>({
		state: 'idle',
		detail: ''
	});

	async function testKey() {
		keyCheck = { state: 'busy', detail: '' };
		try {
			await checkAiConnection({ provider: 'anthropic', apiKey });
			keyCheck = { state: 'ok', detail: '' };
			void engineKey.refresh();
		} catch (e) {
			const detail = e instanceof ApiError ? e.message : locale.t('ai.unreachable');
			keyCheck = { state: 'failed', detail };
		}
	}

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		if (wish.trim() === '') {
			wishError = locale.t('project.wishMissing');
			wishField?.focus();
			return;
		}
		busy = true;
		error = null;
		wishError = null;
		try {
			const project = await createProject(wish, apiKey, name);
			void engineKey.refresh();
			created(project);
		} catch (e) {
			const { key, params } = describeError(e);
			error = locale.t(key, params);
		} finally {
			busy = false;
		}
	}
</script>

<form class="page" onsubmit={submit}>
	<a href="#projects">{locale.t('nav.back')}</a>
	<h1>{locale.t('wish.title')}</h1>
	<p class="lead">{locale.t('wish.lead')}</p>

	<div class="grid">
		<div class="card">
			<label>
				{locale.t('wish.name')}
				<input
					type="text"
					maxlength="120"
					bind:value={name}
					aria-describedby="name-hint"
					data-testid="name"
				/>
				<span class="hint" id="name-hint">{locale.t('wish.nameHint')}</span>
			</label>
			<label>
				{locale.t('wish.wishLabel')}
				<textarea
					rows="9"
					maxlength={MAX_WISH}
					bind:value={wish}
					bind:this={wishField}
					aria-invalid={wishError ? 'true' : undefined}
					aria-describedby="wish-hint wish-error"
					data-testid="wish"></textarea>
				<span class="hint" id="wish-hint">
					{locale.t('wish.wishHint')}
					{locale.t('wish.chars', { count: String(wish.length), max: String(MAX_WISH) })}
				</span>
				<span id="wish-error" role="alert" class="field-error" hidden={!wishError}>{wishError}</span
				>
			</label>
		</div>

		<aside class="card">
			<h2>{locale.t('wish.aiHeading')}</h2>
			<p class="field-title">{locale.t('wish.agent')}</p>
			<div class="agent selected">
				<strong>{locale.t('wish.claudeCode')}</strong>
				<div class="lead">{locale.t('wish.claudeCodeNote')}</div>
			</div>
			{#if engineKey.anySet}
				<p class="ok" role="status" data-testid="key-held">
					{locale.t('key.setHere', { state: locale.t(held.key, held.params) })}
				</p>
			{/if}
			<label class="keylabel">
				{locale.t('wish.keyLabel')}
				<span class="keyrow">
					<input type="password" autocomplete="off" bind:value={apiKey} data-testid="project-key" />
					<button
						type="button"
						onclick={testKey}
						disabled={apiKey === '' || keyCheck.state === 'busy'}
						data-testid="test-key"
					>
						{locale.t('wish.test')}
					</button>
				</span>
			</label>
			{#if keyCheck.state === 'ok'}
				<p class="ok" role="status" data-testid="key-result">{locale.t('wish.keyWorks')}</p>
			{:else if keyCheck.state === 'failed'}
				<p role="alert" data-testid="key-result">
					{locale.t('ai.failed', { detail: keyCheck.detail })}
				</p>
			{/if}
			<p class="hint">{locale.t('wish.keyNote')}</p>
		</aside>
	</div>

	{#if error}<p role="alert">{error}</p>{/if}
	<footer>
		<span class="lead">{locale.t('wish.next')}</span>
		<button type="submit" class="primary" disabled={busy} data-testid="create">
			{busy ? locale.t('project.creating') : locale.t('wish.send')}
		</button>
	</footer>
</form>

<style>
	.grid {
		display: grid;
		grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
		gap: var(--space-5);
		align-items: start;
	}
	@media (max-width: 52rem) {
		.grid {
			grid-template-columns: minmax(0, 1fr);
		}
	}
	.field-title {
		font-weight: 600;
		margin: 0 0 var(--space-2);
	}
	.agent {
		border: 2px solid var(--line);
		border-radius: var(--radius-sm);
		padding: var(--space-3) var(--space-4);
		margin-bottom: var(--space-4);
	}
	.agent.selected {
		border-color: var(--blue);
		background: var(--blue-soft);
	}
	.agent .lead {
		margin: 0;
	}
	.keyrow {
		display: flex;
		gap: var(--space-2);
	}
	.ok {
		color: var(--green-ink);
	}
	footer {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		align-items: center;
		gap: var(--space-4);
		border-top: 1px solid var(--line);
		padding-top: var(--space-4);
	}
</style>
