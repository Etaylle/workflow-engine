<script lang="ts">
	import {
		answerRunQuestion,
		getLatestRun,
		getRun,
		listRunEvents,
		runStreamUrl,
		runZipUrl,
		startRun,
		type Project,
		type Run,
		type RunEvent
	} from '#lib/api/client.js';
	import RunControls from '#lib/components/RunControls.svelte';
	import RunReportCard from '#lib/components/RunReportCard.svelte';
	import BuildWith from '#lib/components/BuildWith.svelte';
	import StepShell from '#lib/components/StepShell.svelte';
	import { getBuilders, type Builders } from '#lib/api/execution.js';
	import { missingCodes, startBlock } from '#lib/execution/builders.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import {
		canDownload,
		canStart,
		formatUsd,
		hasQuestion,
		isActive,
		lastSeen,
		mergeConsole,
		progress
	} from '#lib/run/run.js';

	let { project, changed, back }: { project: Project; changed: () => Promise<void>; back: string } =
		$props();

	let run = $state<Run | null>(null);
	let console_ = $state<RunEvent[]>([]);
	const loading = createAction();
	const starting = createAction();
	const answering = createAction();
	let hint = $state('');
	let builders = $state<Builders | null>(null);
	const block = $derived(startBlock(builders, project.agentHarness));
	const blockReason = $derived(
		block && canStart(project.status, run)
			? locale.t('build.blocked', {
					what: missingCodes(block)
						.map((code) =>
							locale.has(`build.code.${code}`) ? locale.t(`build.code.${code}` as MessageKey) : code
						)
						.join(' ')
				})
			: null
	);

	async function loadBuilders() {
		builders = await getBuilders();
	}
	const active = $derived(isActive(run));

	async function load() {
		await loading.run(async () => {
			run = await getLatestRun(project.id);
			console_ = [];
			if (run) console_ = mergeConsole([], await listRunEvents(run.id, 0));
		});
	}

	async function start() {
		const ok = await starting.run(async () => {
			run = await startRun(project.id);
			console_ = [];
		});
		if (ok) changed();
		else await loadBuilders();
	}

	async function answer(choice: 'retry' | 'skip' | 'stop') {
		const question = run?.question;
		if (!run || !question) return;
		const ok = await answering.run(async () => {
			run = await answerRunQuestion(run!.id, question.id, choice, hint);
		});
		if (ok) hint = '';
	}

	async function refresh(runId: string) {
		const fresh = await getRun(runId);
		run = fresh;
		if (!isActive(fresh)) changed();
	}

	/** Follows an active run: the stream replays from the last number seen, then goes live. */
	function follow(runId: string, after: number): () => void {
		let seen = after;
		const source = new EventSource(runStreamUrl(runId, after));
		const onEvent = (message: MessageEvent<string>) => {
			const event = JSON.parse(message.data) as RunEvent;
			if (event.seq <= seen) return;
			seen = lastSeen([event], seen);
			console_ = mergeConsole(console_, [event]);
			if (event.kind !== 'console') void refresh(runId);
		};
		for (const kind of ['run', 'task', 'step', 'console']) {
			source.addEventListener(kind, onEvent as EventListener);
		}
		return () => source.close();
	}

	$effect(() => {
		void project.id;
		void load();
		void loadBuilders();
	});

	$effect(() => {
		const current = run;
		if (!current || !isActive(current)) return;
		return follow(current.id, lastSeen(console_, 0) > 0 ? lastSeen(console_, 0) : 0);
	});

	const statusKey = (status: string) => `run.status.${status}` as MessageKey;
	const taskKey = (status: string) => `run.task.${status}` as MessageKey;
	const stepKey = (status: string) => `run.step.${status}` as MessageKey;
</script>

<StepShell
	step="run"
	heading={locale.t('run.heading')}
	lead={locale.t('step.runLead')}
	{back}
	reason={blockReason}
>
	{#if loading.error}<p role="alert">{loading.error}</p>{/if}
	{#if builders && canStart(project.status, run)}
		<BuildWith {project} {builders} update={(fresh) => (builders = fresh)} {changed} />
	{/if}
	{#if starting.error}
		<p role="alert" data-testid="run-error">
			{starting.error}
			{#if builders}{locale.t('build.errorChoose')}{/if}
		</p>
	{/if}

	{#if run}
		{@const steps = progress(run)}
		<div class="card" data-testid="run-card">
			<p>
				<span class="pill" data-testid="run-status" data-status={run.status}>
					{locale.t(statusKey(run.status))}
				</span>
				<span data-testid="run-progress">
					{locale.t('run.progress', { done: String(steps.done), total: String(steps.total) })}
				</span>
			</p>
			<p data-testid="run-builder">
				{locale.t('run.builtWith', { builder: locale.t(`build.${run.harness}` as MessageKey) })}
			</p>
			<p data-testid="run-cost">
				{locale.t('run.cost', {
					cost: formatUsd(run.costUsd),
					limit: formatUsd(run.costLimitUsd)
				})}
			</p>
			{#if run.status === 'interrupted'}
				<p role="alert" data-testid="run-reason">
					{locale.t('run.interruptedHint')}
					{#if run.failureReason}<br />{run.failureReason}{/if}
				</p>
			{/if}
			{#if run.status === 'waiting' && run.waitKind !== 'question'}
				<p role="status" data-testid="run-wait" data-kind={run.waitKind}>
					{locale.t(`run.wait.${run.waitKind}` as MessageKey)}
					{#if run.failureReason}<br />{run.failureReason}{/if}
				</p>
			{/if}
			{#if hasQuestion(run) && run.question}
				<div class="question" role="alert" data-testid="run-question">
					<h3>{locale.t('run.question.heading', { step: run.question.stepTitle })}</h3>
					<p data-testid="question-text">{run.question.text}</p>
					<label>
						{locale.t('run.question.hint')}
						<textarea rows="2" maxlength="1000" bind:value={hint} data-testid="question-hint"
						></textarea>
					</label>
					<div class="actions">
						<button
							class="primary"
							onclick={() => answer('retry')}
							disabled={answering.busy}
							data-testid="answer-retry"
						>
							{locale.t('run.question.retry')}
						</button>
						<button
							onclick={() => answer('skip')}
							disabled={answering.busy}
							data-testid="answer-skip"
						>
							{locale.t('run.question.skip')}
						</button>
						<button
							onclick={() => answer('stop')}
							disabled={answering.busy}
							data-testid="answer-stop"
						>
							{locale.t('run.question.stop')}
						</button>
					</div>
					<p class="notice">{locale.t('run.question.skipNote')}</p>
					{#if answering.error}<p role="alert" data-testid="answer-error">{answering.error}</p>{/if}
				</div>
			{/if}

			<ol class="tasks" data-testid="run-tasks">
				{#each run.tasks as task (task.id)}
					<li data-testid={`run-task-${task.seq}`} data-status={task.status}>
						<strong>{task.title}</strong> · {locale.t(taskKey(task.status))}
						<ul>
							{#each task.steps as step (step.id)}
								<li data-status={step.status}>
									{step.title} · {locale.t(stepKey(step.status))}
									{#if step.costUsd > 0}
										({locale.t('run.stepCost', {
											cost: formatUsd(step.costUsd)
										})}){:else if step.tokens > 0}
										({locale.t('run.stepTokens', {
											tokens: step.tokens.toLocaleString(locale.current)
										})}){/if}
									{#if step.attempts > 1}
										<span data-testid={`attempts-${step.key}`}>
											{locale.t('run.attempts', { count: String(step.attempts) })}
										</span>
									{/if}
								</li>
							{/each}
						</ul>
					</li>
				{/each}
			</ol>

			<RunControls
				{run}
				updated={(fresh) => {
					run = fresh;
					if (!isActive(fresh)) changed();
				}}
			/>

			<h3>{locale.t('run.console')}</h3>
			<pre class="console" data-testid="run-console" aria-live="polite">{console_
					.map((line) => line.text)
					.join('\n')}</pre>

			{#if !active}<RunReportCard {run} />{/if}

			{#if active}<p class="notice">{locale.t('run.reloadHint')}</p>{/if}
		</div>
	{:else if !loading.busy}
		<div class="empty">
			<h3>{locale.t('run.none')}</h3>
			<p>{locale.t('run.noneNext')}</p>
		</div>
	{/if}

	{#if run && canDownload(run) && canStart(project.status, run)}
		<div class="optional">
			<span class="optional-label">{locale.t('step.optional')}</span>
			<button
				class="quiet"
				onclick={start}
				disabled={starting.busy || block !== null}
				data-testid="start-run"
			>
				{starting.busy ? locale.t('run.starting') : locale.t('run.startAgain')}
			</button>
		</div>
	{/if}

	{#snippet primary()}
		{#if run && canDownload(run)}
			<a class="button primary" href={runZipUrl(run.id)} data-testid="run-zip">
				{locale.t('run.zip')}
			</a>
		{:else if canStart(project.status, run)}
			<button
				class="primary"
				onclick={start}
				disabled={starting.busy || block !== null}
				data-testid="start-run"
			>
				{starting.busy ? locale.t('run.starting') : locale.t(run ? 'run.startAgain' : 'run.start')}
			</button>
		{/if}
	{/snippet}
</StepShell>

<style>
	.tasks {
		padding-left: 1.2rem;
	}
	.console {
		max-height: 16rem;
		overflow: auto;
		background: var(--grey-soft);
		border-radius: var(--radius-sm);
		padding: var(--space-3);
		font-size: var(--text-xs);
		white-space: pre-wrap;
	}
	[data-status='failed'] {
		color: var(--red);
	}
	[data-status='done'] {
		color: var(--green-ink);
	}
	.question {
		border: 1px solid var(--field-line);
		border-radius: 8px;
		padding: 0.75rem;
		margin: 0.75rem 0;
	}
	.question textarea {
		display: block;
		width: 100%;
	}
	.actions {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		margin-top: 0.5rem;
	}
</style>
