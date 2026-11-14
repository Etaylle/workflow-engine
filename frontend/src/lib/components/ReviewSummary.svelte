<script lang="ts">
	import type { Project } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import type { ReviewFacts } from '#lib/planning/review.js';

	let { project, facts }: { project: Project; facts: ReviewFacts } = $props();

	const harness = $derived(
		project.agentHarness === 'claude_code' ? 'Claude Code' : project.agentHarness
	);
</script>

<div class="card" data-testid="review-summary">
	<h3>{locale.t('review.project')}</h3>
	<dl>
		<dt>{locale.t('review.wish')}</dt>
		<dd>{project.name}</dd>
		<dt>{locale.t('review.answers')}</dt>
		<dd>
			{locale.t('review.answersValue', {
				answers: String(facts.answers),
				byAi: String(facts.byAi)
			})}
		</dd>
		<dt>{locale.t('review.technology')}</dt>
		<dd>Java + Svelte + PostgreSQL</dd>
		<dt>{locale.t('review.ai')}</dt>
		<dd>{harness} · {project.model}</dd>
		<dt>{locale.t('review.requirements')}</dt>
		<dd>
			{locale.t('review.requirementsValue', {
				total: String(facts.requirements),
				must: String(facts.must),
				should: String(facts.should)
			})}
		</dd>
		<dt>{locale.t('review.workflow')}</dt>
		<dd>
			{locale.t('review.workflowValue', {
				tasks: String(facts.tasks),
				software: String(facts.software),
				manual: String(facts.manual)
			})}
		</dd>
	</dl>
	<p class="notice">{locale.t('review.noLogin')}</p>
</div>

<style>
	dl {
		display: grid;
		grid-template-columns: minmax(8rem, 12rem) 1fr;
		gap: 0;
		margin: 0;
	}
	dt,
	dd {
		margin: 0;
		padding: 0.6rem 0;
		border-top: 1px solid var(--line);
	}
	dt {
		color: var(--muted);
	}
</style>
