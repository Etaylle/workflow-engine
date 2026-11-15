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

	let { project, changed }: { project: Project; changed: () => void } = $props();

	let run = $state<Run | null>(null);
	let console_ = $state<RunEvent[]>([]);
	const loading = createAction();
	const starting = createAction();
	const answering = createAction();
	let hint = $state('');
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
	}

	async function answer(choice: 'retry' | 'skip') {
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

<section aria-labelledby="run-heading" data-testid="step-run">
	<h2 id="run-heading">{locale.t('run.heading')}</h2>
	{#if loading.error}<p role="alert">{loading.error}</p>{/if}

	{#if canStart(project.status, run)}
		<button class="primary" onclick={start} disabled={starting.busy} data-testid="start-run">
			{starting.busy ? locale.t('run.starting') : locale.t(run ? 'run.startAgain' : 'run.start')}
		</button>
	{/if}
	{#if starting.error}<p role="alert" data-testid="run-error">{starting.error}</p>{/if}

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
										({locale.t('run.stepCost', { cost: formatUsd(step.costUsd) })}){/if}
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

			<h3>{locale.t('run.console')}</h3>
			<pre class="console" data-testid="run-console" aria-live="polite">{console_
					.map((line) => line.text)
					.join('\n')}</pre>

			{#if canDownload(run)}
				<a class="button" href={runZipUrl(run.id)} data-testid="run-zip">
					{locale.t('run.zip')}
				</a>
			{/if}
			{#if active}<p class="notice">{locale.t('run.reloadHint')}</p>{/if}
		</div>
	{:else if !loading.busy}
		<p>{locale.t('run.none')}</p>
	{/if}
</section>

<style>
	.tasks {
		padding-left: 1.2rem;
	}
	.console {
		max-height: 16rem;
		overflow: auto;
		background: var(--grey-soft);
		border-radius: 8px;
		padding: 0.75rem;
		font-size: 0.85rem;
		white-space: pre-wrap;
	}
	[data-status='failed'] {
		color: #a12121;
	}
	[data-status='done'] {
		color: var(--green-ink);
	}
	.question {
		border: 1px solid #c9c5bb;
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
	a.button {
		display: inline-block;
		padding: 0.45rem 1rem;
		border: 1px solid #c9c5bb;
		border-radius: 8px;
		text-decoration: none;
		background: var(--surface);
	}
</style>
