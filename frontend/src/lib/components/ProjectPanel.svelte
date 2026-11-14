<script lang="ts">
	import type { Project } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { rank } from '#lib/planning/steps.js';
	import WishEditor from './WishEditor.svelte';
	import QuestionsStep from './QuestionsStep.svelte';
	import RequirementsStep from './RequirementsStep.svelte';
	import WorkflowPanel from './WorkflowPanel.svelte';

	let { project, changed }: { project: Project; changed: () => void } = $props();
</script>

<article class="page" aria-labelledby="project-heading">
	<a href="#projects">{locale.t('nav.back')}</a>
	<h1 id="project-heading">{project.name}</h1>
	<WishEditor {project} {changed} />

	<QuestionsStep {project} {changed} />
	{#if rank(project.status) >= 2}
		<RequirementsStep {project} {changed} />
	{/if}
	{#if rank(project.status) >= 3}
		<WorkflowPanel {project} {changed} />
	{/if}
</article>
