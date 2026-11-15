<script lang="ts">
	import { untrack } from 'svelte';
	import {
		answerRunQuestion,
		getLatestRun,
		getRun,
		getRunPreview,
		listOlderEvents,
		runStreamUrl,
		runZipUrl,
		startRun,
		startRunPreview,
		stopRunPreview,
		updateProject,
		type Preview,
		type Project,
		type Run,
		type RunEvent
	} from '#lib/api/client.js';
	import ConsolePane from '#lib/components/ConsolePane.svelte';
	import FilesPane from '#lib/components/FilesPane.svelte';
	import PreviewPane from '#lib/components/PreviewPane.svelte';
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
		canStop,
		lastSeen,
		mergeConsole,
		OLDER_PAGE,
		oldestSeq,
		prependOlder,
		progress
	} from '#lib/run/run.js';
	import { previewActive, previewShown } from '#lib/run/preview.js';
	import { availableTabs, canPreview, effectiveTab, type RunTab } from '#lib/run/tabs.js';

	let { project, changed, back }: { project: Project; changed: () => Promise<void>; back: string } =
		$props();

	let run = $state<Run | null>(null);
	let console_ = $state<RunEvent[]>([]);
	let hasOlder = $state(false);
	let chosenTab = $state<RunTab>('activity');
	let confirmingStop = $state(false);
	const older = createAction();
	let preview = $state<Preview | null>(null);
	let appLines = $state<RunEvent[]>([]);
	let appHasOlder = $state(false);
	const previewing = createAction();
	const olderApp = createAction();
	const tabs = $derived(availableTabs(run, previewShown(preview)));
	const tab = $derived(effectiveTab(chosenTab, tabs));
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

	async function chooseBuilder(id: string) {
		await updateProject(project.id, { agentHarness: id });
		await changed();
	}

	async function loadBuilders() {
		builders = await getBuilders();
	}
	const active = $derived(isActive(run));

	async function load() {
		await loading.run(async () => {
			run = await getLatestRun(project.id);
			console_ = [];
			hasOlder = false;
			if (run) {
				const page = await listOlderEvents(run.id, Number.MAX_SAFE_INTEGER, 'console', OLDER_PAGE);
				console_ = mergeConsole([], page);
				hasOlder = page.length === OLDER_PAGE;
				await loadPreview(run.id);
			}
		});
	}

	async function loadPreview(runId: string) {
		preview = await getRunPreview(runId);
		appLines = [];
		appHasOlder = false;
		if (previewShown(preview)) {
			const page = await listOlderEvents(runId, Number.MAX_SAFE_INTEGER, 'app', OLDER_PAGE);
			appLines = mergeConsole([], page, 'app');
			appHasOlder = page.length === OLDER_PAGE;
		}
	}

	async function startPreview() {
		if (!run) return;
		const runId = run.id;
		const ok = await previewing.run(async () => {
			appLines = [];
			preview = await startRunPreview(runId);
		});
		if (ok) chosenTab = 'app';
	}

	async function stopPreview() {
		if (!run) return;
		const runId = run.id;
		await previewing.run(async () => {
			preview = await stopRunPreview(runId);
		});
	}

	async function loadOlderApp() {
		const before = oldestSeq(appLines);
		if (!run || before === null) return;
		const runId = run.id;
		await olderApp.run(async () => {
			const page = await listOlderEvents(runId, before, 'app', OLDER_PAGE);
			appLines = prependOlder(appLines, page);
			appHasOlder = page.length === OLDER_PAGE;
		});
	}

	async function loadOlder() {
		const before = oldestSeq(console_);
		if (!run || before === null) return;
		const runId = run.id;
		await older.run(async () => {
			const page = await listOlderEvents(runId, before, 'console', OLDER_PAGE);
			console_ = prependOlder(console_, page);
			hasOlder = page.length === OLDER_PAGE;
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
			if (event.kind === 'app') {
				appLines = mergeConsole(appLines, [event], 'app');
				return;
			}
			if (event.kind === 'preview') {
				void getRunPreview(runId).then((fresh) => (preview = fresh));
				return;
			}
			console_ = mergeConsole(console_, [event]);
			if (event.kind !== 'console') void refresh(runId);
		};
		for (const kind of ['run', 'task', 'step', 'console', 'app', 'preview']) {
			source.addEventListener(kind, onEvent as EventListener);
		}
		return () => source.close();
	}

	$effect(() => {
		void project.id;
		void load();
		void loadBuilders();
	});

	/** The run to follow live; a stable value, so the stream is not reopened on every new line. */
	const followId = $derived(run && (isActive(run) || previewActive(preview)) ? run.id : null);

	$effect(() => {
		const id = followId;
		if (!id) return;
		return follow(
			id,
			untrack(() => lastSeen(console_, 0))
		);
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
	<p class="hint" data-testid="run-build-mode">
		{locale.t('run.buildModeLine', {
			mode: locale.t(`buildMode.${project.buildMode ?? 'auto'}Short`)
		})}
	</p>
	{#if loading.error}<p role="alert">{loading.error}</p>{/if}
	{#if builders && canStart(project.status, run) && !run}
		<BuildWith
			harness={project.agentHarness}
			{builders}
			update={(fresh) => (builders = fresh)}
			choose={chooseBuilder}
		/>
	{/if}
	{#if starting.error}
		<p role="alert" data-testid="run-error">
			{starting.error}
			{#if builders}{locale.t('build.errorChoose')}{/if}
		</p>
	{/if}

	{#if run}
		{@const steps = progress(run)}
		<div class="run" data-testid="run-card">
			<div class="facts">
				<span class="pill" data-testid="run-status" data-status={run.status}>
					{locale.t(statusKey(run.status))}
				</span>
				<span data-testid="run-progress">
					{locale.t('run.progress', { done: String(steps.done), total: String(steps.total) })}
				</span>
				<span data-testid="run-builder">
					{locale.t('run.builtWith', { builder: locale.t(`build.${run.harness}` as MessageKey) })}
				</span>
				<span data-testid="run-cost">
					{locale.t('run.cost', {
						cost: formatUsd(run.costUsd),
						limit: formatUsd(run.costLimitUsd)
					})}
				</span>
			</div>
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

			<RunControls
				{run}
				bind:confirming={confirmingStop}
				updated={(fresh) => {
					run = fresh;
					if (!isActive(fresh)) changed();
				}}
			/>

			<div class="watch">
				<div class="side">
					<h3>{locale.t('run.tasks')}</h3>
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
				</div>
				<div class="main">
					<div class="tabs" role="tablist" aria-label={locale.t('run.tabs')}>
						{#each tabs as name (name)}
							<button
								type="button"
								role="tab"
								class:current={tab === name}
								aria-selected={tab === name}
								onclick={() => (chosenTab = name)}
								data-testid={`tab-${name}`}
							>
								{locale.t(`run.tab.${name}` as MessageKey)}
							</button>
						{/each}
					</div>
					<div class="tabpanel" role="tabpanel">
						{#if tab === 'activity'}
							<ConsolePane
								lines={console_}
								testid="run-console"
								label={locale.t('run.tab.activity')}
								empty={locale.t('run.activityEmpty')}
								{hasOlder}
								loadingOlder={older.busy}
								{loadOlder}
							/>
						{:else if tab === 'files'}
							<FilesPane runId={run.id} version={steps.done} />
						{:else if tab === 'app' && preview}
							<PreviewPane
								{preview}
								lines={appLines}
								hasOlder={appHasOlder}
								loadingOlder={olderApp.busy}
								loadOlder={loadOlderApp}
								stop={stopPreview}
								stopping={previewing.busy}
							/>
						{/if}
					</div>
				</div>
			</div>

			{#if !active}<RunReportCard {run} />{/if}

			{#if active}<p class="notice">{locale.t('run.reloadHint')}</p>{/if}
		</div>
	{:else if !loading.busy}
		<div class="empty">
			<h3>{locale.t('run.none')}</h3>
			<p>{locale.t('run.noneNext')}</p>
		</div>
	{/if}

	{#if builders && run && canStart(project.status, run)}
		<BuildWith
			harness={project.agentHarness}
			{builders}
			update={(fresh) => (builders = fresh)}
			choose={chooseBuilder}
		/>
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
		{#if previewing.error}
			<span class="reason" role="alert" data-testid="preview-error">{previewing.error}</span>
		{/if}
		{#if canPreview(run) && !previewActive(preview)}
			<button
				onclick={startPreview}
				disabled={previewing.busy}
				title={locale.t('preview.hint')}
				data-testid="start-preview"
			>
				{previewing.busy ? locale.t('preview.starting') : locale.t('preview.start')}
			</button>
		{/if}
		{#if canStart(project.status, run) && !(run && canDownload(run))}
			<button
				class="primary"
				onclick={start}
				disabled={starting.busy || block !== null}
				data-testid="start-run"
			>
				{starting.busy ? locale.t('run.starting') : locale.t(run ? 'run.startAgain' : 'run.start')}
			</button>
		{:else if run && canStop(run)}
			<button class="danger solid" onclick={() => (confirmingStop = true)} data-testid="stop-run">
				{locale.t('run.stop')}
			</button>
		{:else if run && canDownload(run)}
			<a class="button primary" href={runZipUrl(run.id)} data-testid="run-zip">
				{locale.t('run.zip')}
			</a>
		{/if}
	{/snippet}
</StepShell>

<style>
	.reason {
		color: var(--amber-ink);
		font-size: var(--text-sm);
	}
	.facts {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: var(--space-2) var(--space-5);
		margin-bottom: var(--space-4);
		color: var(--muted);
	}
	.watch {
		display: grid;
		grid-template-columns: minmax(15rem, 22rem) minmax(0, 1fr);
		gap: var(--space-5);
		height: clamp(24rem, calc(100vh - 27rem), 70rem);
		margin-top: var(--space-4);
	}
	.side {
		overflow: auto;
		min-height: 0;
	}
	.main {
		display: flex;
		flex-direction: column;
		min-height: 0;
		min-width: 0;
	}
	.tabs {
		display: flex;
		gap: var(--space-1);
		border-bottom: 1px solid var(--line);
		margin-bottom: var(--space-3);
	}
	.tabs button {
		border: 0;
		border-bottom: 3px solid transparent;
		border-radius: 0;
		background: transparent;
		font-weight: 500;
	}
	.tabs button.current {
		border-bottom-color: var(--blue);
		color: var(--blue-ink);
	}
	.tabpanel {
		display: flex;
		flex-direction: column;
		flex: 1 1 auto;
		min-height: 0;
	}
	@media (max-width: 56rem) {
		.watch {
			grid-template-columns: minmax(0, 1fr);
			height: auto;
		}
		.tabpanel {
			height: 28rem;
		}
	}
	.tasks {
		padding-left: 1.2rem;
		margin: 0;
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
