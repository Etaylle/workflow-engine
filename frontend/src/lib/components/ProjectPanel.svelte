<script lang="ts">
	import type { Project } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { currentStep, rank, STEPS } from '#lib/planning/steps.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import QuestionsStep from './QuestionsStep.svelte';
	import RequirementsStep from './RequirementsStep.svelte';
	import WorkflowPanel from './WorkflowPanel.svelte';

	let { project, changed }: { project: Project; changed: () => void } = $props();

	const current = $derived(currentStep(project.status));
	const stepKey = (step: string) => `step.${step}` as MessageKey;
</script>

<article aria-labelledby="project-heading">
	<h2 id="project-heading">{project.name}</h2>
	<p data-testid="wish-text">{project.wish}</p>
	<ol class="steps" data-testid="steps">
		{#each STEPS as step (step)}
			<li aria-current={step === current ? 'step' : undefined}>{locale.t(stepKey(step))}</li>
		{/each}
	</ol>

	<QuestionsStep {project} {changed} />
	{#if rank(project.status) >= 2}
		<RequirementsStep {project} {changed} />
	{/if}
	{#if rank(project.status) >= 3}
		<WorkflowPanel {project} {changed} />
	{/if}
</article>

<style>
	.steps {
		display: flex;
		gap: 1.5rem;
		padding: 0;
		list-style-position: inside;
	}
	.steps li[aria-current='step'] {
		font-weight: 700;
		text-decoration: underline;
	}
</style>
