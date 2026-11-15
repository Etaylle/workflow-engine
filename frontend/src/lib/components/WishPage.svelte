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
	const held = $derived(describeKey(engineKey.state));
	let keyCheck = $state<{ state: 'idle' | 'busy' | 'ok' | 'failed'; detail: string }>({
		state: 'idle',
		detail: ''
	});

	async function testKey() {
		keyCheck = { state: 'busy', detail: '' };
		try {
			await checkAiConnection(apiKey);
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
			error = locale.t('project.wishMissing');
			return;
		}
		busy = true;
		error = null;
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
				<input type="text" maxlength="120" bind:value={name} data-testid="name" />
			</label>
			<label>
				{locale.t('wish.wishLabel')}
				<textarea rows="9" maxlength={MAX_WISH} bind:value={wish} data-testid="wish"></textarea>
			</label>
			<p class="lead">
				{locale.t('wish.chars', { count: String(wish.length), max: String(MAX_WISH) })}
			</p>
		</div>

		<aside class="card">
			<h2>{locale.t('wish.aiHeading')}</h2>
			<p class="field-title">{locale.t('wish.agent')}</p>
			<div class="agent selected">
				<strong>{locale.t('wish.claudeCode')}</strong>
				<div class="lead">{locale.t('wish.claudeCodeNote')}</div>
			</div>
			{#if engineKey.state?.set}
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
			<p class="lead">{locale.t('wish.keyNote')}</p>
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
		gap: 1.25rem;
		align-items: start;
	}
	@media (max-width: 52rem) {
		.grid {
			grid-template-columns: minmax(0, 1fr);
		}
	}
	.field-title {
		font-weight: 500;
		margin: 0 0 0.4rem;
	}
	.agent {
		border: 2px solid var(--line);
		border-radius: 10px;
		padding: 0.6rem 0.9rem;
		margin-bottom: 1rem;
	}
	.agent.selected {
		border-color: var(--blue);
		background: var(--blue-soft);
	}
	.keyrow {
		display: flex;
		gap: 0.5rem;
	}
	.ok {
		color: var(--green-ink);
	}
	footer {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		align-items: center;
		gap: 1rem;
		border-top: 1px solid var(--line);
		padding-top: 1rem;
	}
	footer .primary {
		padding: 0.7rem 1.4rem;
	}
</style>
