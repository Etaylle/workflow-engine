<script lang="ts">
	import { onMount } from 'svelte';
	import { ApiError, checkAiConnection, getStatus, type EngineStatus } from '#lib/api/client.js';
	import { LOCALES } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	type Load = { state: 'loading' } | { state: 'error' } | { state: 'ready'; status: EngineStatus };
	type Check =
		| { state: 'idle' }
		| { state: 'busy' }
		| { state: 'ok'; model: string }
		| { state: 'failed'; detail: string };

	let load = $state<Load>({ state: 'loading' });
	let check = $state<Check>({ state: 'idle' });
	let apiKey = $state('');

	onMount(async () => {
		locale.init();
		try {
			load = { state: 'ready', status: await getStatus() };
		} catch {
			load = { state: 'error' };
		}
	});

	async function runCheck(event: SubmitEvent) {
		event.preventDefault();
		check = { state: 'busy' };
		try {
			const result = await checkAiConnection(apiKey);
			check = { state: 'ok', model: result.model };
			apiKey = '';
		} catch (error) {
			const detail = error instanceof ApiError ? error.message : locale.t('ai.unreachable');
			check = { state: 'failed', detail };
		}
	}
</script>

<svelte:head><title>{locale.t('app.pageTitle')}</title></svelte:head>

<main>
	<header>
		<h1>{locale.t('app.title')}</h1>
		<label>
			{locale.t('language.label')}
			<select
				value={locale.current}
				onchange={(e) => locale.set(e.currentTarget.value === 'de' ? 'de' : 'en')}
				data-testid="language"
			>
				{#each LOCALES as code (code)}
					<option value={code}>{code.toUpperCase()}</option>
				{/each}
			</select>
		</label>
	</header>

	<section aria-labelledby="status-heading">
		<h2 id="status-heading">{locale.t('status.heading')}</h2>
		{#if load.state === 'loading'}
			<p>{locale.t('status.loading')}</p>
		{:else if load.state === 'error'}
			<p role="alert">{locale.t('status.error')}</p>
		{:else}
			<dl data-testid="status">
				<dt>{locale.t('status.name')}</dt>
				<dd>{load.status.name}</dd>
				<dt>{locale.t('status.build')}</dt>
				<dd>{load.status.buildId}</dd>
				<dt>{locale.t('status.time')}</dt>
				<dd>{load.status.serverTime}</dd>
			</dl>
		{/if}
	</section>

	<section aria-labelledby="ai-heading">
		<h2 id="ai-heading">{locale.t('ai.heading')}</h2>
		<p>{locale.t('ai.explain')}</p>
		<form onsubmit={runCheck}>
			<label>
				{locale.t('ai.keyLabel')}
				<input type="password" autocomplete="off" bind:value={apiKey} required />
			</label>
			<button type="submit" disabled={check.state === 'busy'}>
				{check.state === 'busy' ? locale.t('ai.checking') : locale.t('ai.check')}
			</button>
		</form>
		{#if check.state === 'ok'}
			<p role="status" data-testid="ai-result">{locale.t('ai.ok', { model: check.model })}</p>
		{:else if check.state === 'failed'}
			<p role="alert" data-testid="ai-result">{locale.t('ai.failed', { detail: check.detail })}</p>
		{/if}
	</section>
</main>

<style>
	main {
		max-width: 40rem;
		margin: 2rem auto;
		padding: 0 1rem;
		font-family: system-ui, sans-serif;
	}
	header {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	dl {
		display: grid;
		grid-template-columns: max-content 1fr;
		gap: 0.25rem 1rem;
	}
	input {
		display: block;
		width: 100%;
		margin: 0.25rem 0 0.5rem;
	}
</style>
