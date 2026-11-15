<script lang="ts">
	import { getRunReport, type Run, type RunReport } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { formatMinutes, formatUsd } from '#lib/run/run.js';

	let { run }: { run: Run } = $props();

	let report = $state<RunReport | null>(null);
	const loading = createAction();

	$effect(() => {
		// reload when the run changes: a new status, cost or answer
		void [run.id, run.status, run.costUsd, run.lastSeq];
		void loading.run(async () => {
			report = await getRunReport(run.id);
		});
	});
</script>

<section class="report" data-testid="run-report" aria-labelledby="report-heading">
	<h3 id="report-heading">{locale.t('run.report.heading')}</h3>
	{#if loading.error && !report}<p role="alert">{locale.t('run.report.failed')}</p>{/if}
	{#if report}
		<ul>
			<li data-testid="report-time">
				{locale.t('run.report.time', { minutes: formatMinutes(report.seconds) })}
			</li>
			<li data-testid="report-cost">
				{locale.t('run.report.cost', {
					cost: formatUsd(report.costUsd),
					limit: formatUsd(report.costLimitUsd)
				})}
			</li>
			<li data-testid="report-tasks">
				{locale.t('run.report.tasks', {
					done: String(report.tasksDone),
					total: String(report.tasksTotal)
				})}
			</li>
			<li data-testid="report-steps">
				{locale.t('run.report.steps', {
					done: String(report.stepsDone),
					total: String(report.stepsTotal)
				})}
			</li>
			<li>{locale.t('run.report.attempts', { count: String(report.attempts) })}</li>
			<li data-testid="report-checkpoints">
				{locale.t('run.report.checkpoints', { count: String(report.checkpoints) })}
			</li>
			{#if report.resumeCount > 0}
				<li>{locale.t('run.report.resumes', { count: String(report.resumeCount) })}</li>
			{/if}
		</ul>
		<h4>{locale.t('run.report.questions')}</h4>
		{#if report.questions.length === 0}
			<p data-testid="report-no-questions">{locale.t('run.report.noQuestions')}</p>
		{:else}
			<ul data-testid="report-questions">
				{#each report.questions as q (q.askedAt + q.question)}
					<li>
						<strong>{q.stepTitle}</strong>: {q.question}
						{q.choice
							? locale.t('run.report.answer', { choice: q.choice })
							: locale.t('run.report.open')}
						{#if q.hint}({q.hint}){/if}
					</li>
				{/each}
			</ul>
		{/if}
		<p class="notice">{locale.t('run.report.zipNote')}</p>
	{/if}
</section>

<style>
	.report {
		margin-top: 1rem;
		border-top: 1px solid #c9c5bb;
	}
</style>
