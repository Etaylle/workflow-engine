<script lang="ts">
	import type { Project } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { rank } from '#lib/planning/steps.js';
	import DocumentsPanel from './DocumentsPanel.svelte';
	import WishEditor from './WishEditor.svelte';
	import RunPanel from './RunPanel.svelte';
	import QuestionsStep from './QuestionsStep.svelte';
	import RequirementsStep from './RequirementsStep.svelte';
	import WorkflowPanel from './WorkflowPanel.svelte';

	let { project, changed }: { project: Project; changed: () => void } = $props();
</script>

<article class="page" aria-labelledby="project-heading">
	<a href="#projects">{locale.t('nav.back')}</a>
	<h1 id="project-heading">{project.name}</h1>
	<div class="wish card">
		<h2>{locale.t('questions.yourWish')}</h2>
		<WishEditor {project} {changed} />
	</div>

	<QuestionsStep {project} {changed} />
	{#if rank(project.status) >= 2}
		<RequirementsStep {project} {changed} />
	{/if}
	{#if rank(project.status) >= 3}
		<WorkflowPanel {project} {changed} />
	{/if}
	{#if rank(project.status) >= 5}
		<RunPanel {project} {changed} />
	{/if}
	<DocumentsPanel {project} />
</article>

<style>
	.wish h2 {
		font-size: 0.85rem;
		text-transform: uppercase;
		letter-spacing: 0.06em;
		color: var(--muted);
		margin-bottom: 0.4rem;
	}
	.wish :global(.lead) {
		color: var(--ink);
		margin-bottom: 0.9rem;
		white-space: pre-wrap;
	}
</style>
