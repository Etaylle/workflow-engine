<script lang="ts">
	import type { Project } from '#lib/api/client.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	let {
		projects,
		failed,
		selectedId,
		open
	}: {
		projects: Project[];
		failed: boolean;
		selectedId: string | null;
		open: (id: string) => void;
	} = $props();

	const KNOWN = [
		'draft',
		'questions_ready',
		'answered',
		'requirements_ready',
		'workflow_drafted',
		'confirmed'
	];
	const statusKey = (status: string): MessageKey =>
		KNOWN.includes(status) ? (`status.${status}` as MessageKey) : 'status.other';
</script>

<section aria-labelledby="projects-heading">
	<h2 id="projects-heading">{locale.t('projects.heading')}</h2>
	{#if failed}
		<p role="alert">{locale.t('projects.loadFailed')}</p>
	{:else if projects.length === 0}
		<p>{locale.t('projects.empty')}</p>
	{:else}
		<ul data-testid="projects">
			{#each projects as project (project.id)}
				<li>
					<button onclick={() => open(project.id)} aria-current={project.id === selectedId}>
						{locale.t('projects.open')}
					</button>
					{project.name}: {locale.t(statusKey(project.status))}
				</li>
			{/each}
		</ul>
	{/if}
</section>
