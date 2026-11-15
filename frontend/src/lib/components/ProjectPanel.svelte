<script lang="ts">
	import type { Project } from '#lib/api/client.js';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { nextStep, previousStep, rank, type StepId } from '#lib/planning/steps.js';
	import { stepHash } from '#lib/shell/route.js';
	import DocumentsPanel from './DocumentsPanel.svelte';
	import QuestionsStep from './QuestionsStep.svelte';
	import RequirementsStep from './RequirementsStep.svelte';
	import RunPanel from './RunPanel.svelte';
	import StepShell from './StepShell.svelte';
	import WishEditor from './WishEditor.svelte';
	import WorkflowPanel from './WorkflowPanel.svelte';

	let { project, step, changed }: { project: Project; step: StepId; changed: () => Promise<void> } =
		$props();

	/** Moves to another step view; the new view opens at its top. */
	const go = (target: StepId) => {
		location.hash = stepHash(project.id, target);
	};
	const backOf = (current: StepId) => {
		const previous = previousStep(current);
		return previous ? stepHash(project.id, previous) : '#projects';
	};
</script>

<article class="page" class:wide={step === 'run'} aria-labelledby="project-heading">
	<a href="#projects">{locale.t('nav.back')}</a>
	<h1 id="project-heading">{project.name}</h1>
	{#if engineKey.keys?.planning && rank(project.status) <= 4}
		<p class="ai-chip" data-testid="planning-ai">
			{locale.t('planning.with', {
				name: engineKey.nameOf(locale.t('key.nameAnthropic'), engineKey.keys.planning.provider),
				model: engineKey.keys.planning.model
			})}
		</p>
	{/if}

	{#if step === 'wish'}
		<StepShell
			step="wish"
			heading={locale.t('step.wishTitle')}
			lead={locale.t('step.wishLead')}
			back={backOf('wish')}
		>
			<WishEditor {project} {changed} />
			{#snippet primary()}
				<a
					class="button primary"
					href={stepHash(project.id, nextStep('wish') ?? 'questions')}
					data-testid="wish-continue"
				>
					{locale.t('step.continueQuestions')}
				</a>
			{/snippet}
		</StepShell>
	{:else if step === 'questions'}
		<QuestionsStep {project} {changed} back={backOf('questions')} {go} />
	{:else if step === 'requirements'}
		<RequirementsStep {project} {changed} back={backOf('requirements')} />
	{:else if step === 'workflow' || step === 'review'}
		<WorkflowPanel {project} {changed} {step} back={backOf(step)} />
	{:else}
		<RunPanel {project} {changed} back={backOf('run')} />
	{/if}
	{#if step === 'review' || step === 'run'}
		<DocumentsPanel {project} />
	{/if}
</article>

<style>
	.wide {
		max-width: 112rem;
	}
	.ai-chip {
		display: inline-block;
		margin: 0 0 var(--space-4);
		padding: 0.15rem var(--space-3);
		border: 1px solid var(--line);
		border-radius: 999px;
		background: var(--surface);
		color: var(--muted);
		font-size: var(--text-sm);
	}
</style>
