<script lang="ts">
	import { onMount } from 'svelte';
	import AllProjects from '#lib/components/AllProjects.svelte';
	import AppHeader from '#lib/components/AppHeader.svelte';
	import StepsPage from '#lib/components/StepsPage.svelte';
	import ProjectPanel from '#lib/components/ProjectPanel.svelte';
	import WishPage from '#lib/components/WishPage.svelte';
	import { getStatus, listProjects, type EngineStatus, type Project } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { parseRoute, type Route } from '#lib/shell/route.js';

	let engine = $state<EngineStatus | null>(null);
	let projects = $state<Project[]>([]);
	let projectsFailed = $state(false);
	let loaded = $state(false);
	let route = $state<Route>({ name: 'list' });
	const selected = $derived(selectedOf(route, projects));

	function selectedOf(current: Route, all: Project[]): Project | null {
		return current.name === 'project' || current.name === 'steps'
			? (all.find((p) => p.id === current.id) ?? null)
			: null;
	}

	async function reloadProjects() {
		try {
			projects = await listProjects();
			projectsFailed = false;
		} catch {
			projectsFailed = true;
		}
		loaded = true;
	}

	async function created(project: Project) {
		await reloadProjects();
		location.hash = project.id;
	}

	onMount(() => {
		locale.init();
		route = parseRoute(location.hash);
		void reloadProjects();
		void getStatus().then(
			(status) => (engine = status),
			() => (engine = null)
		);
		const onHash = () => {
			route = parseRoute(location.hash);
			void reloadProjects();
		};
		window.addEventListener('hashchange', onHash);
		return () => window.removeEventListener('hashchange', onHash);
	});
</script>

<svelte:head><title>{locale.t('app.pageTitle')}</title></svelte:head>

{#if selected}
	<AppHeader projectName={selected.name} status={selected.status} />
{:else}
	<AppHeader />
{/if}

{#if route.name === 'new'}
	<WishPage {created} />
{:else if route.name === 'steps'}
	{#if selected}
		<StepsPage project={selected} />
	{/if}
{:else if route.name === 'project'}
	{#if selected}
		<ProjectPanel project={selected} changed={reloadProjects} />
	{:else if loaded}
		<div class="page">
			<a href="#projects">{locale.t('nav.back')}</a>
			<p role="alert">{locale.t('project.notFound')}</p>
		</div>
	{/if}
{:else}
	<AllProjects {projects} failed={projectsFailed} changed={reloadProjects} />
{/if}

<p class="engine" data-testid="status">
	{#if engine}
		{locale.t('status.footer', { name: engine.name, build: engine.buildId })}
	{:else}
		{locale.t('status.error')}
	{/if}
</p>

<style>
	.engine {
		text-align: center;
		color: var(--muted);
		font-size: 0.85rem;
	}
</style>
