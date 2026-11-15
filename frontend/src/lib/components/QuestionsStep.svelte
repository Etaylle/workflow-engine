<script lang="ts">
	import {
		addQuestion,
		deleteQuestion,
		editQuestion,
		generateBrief,
		generateQuestions,
		getBrief,
		getQuestions,
		saveAnswers,
		type Brief,
		type Project,
		type Question
	} from '#lib/api/client.js';
	import ConfirmDialog from '#lib/components/ConfirmDialog.svelte';
	import { toasts } from '#lib/shell/toast.svelte.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import {
		asText,
		firstIncomplete,
		initialDraft,
		toggle,
		toRequest,
		type Draft
	} from '#lib/planning/answers.js';
	import { rank, type StepId } from '#lib/planning/steps.js';
	import { stepHash } from '#lib/shell/route.js';
	import StepShell from './StepShell.svelte';
	import {
		editorFrom,
		emptyEditor,
		toInput,
		type EditorState
	} from '#lib/planning/questionEditor.js';
	import QuestionEditor from './QuestionEditor.svelte';
	import Waiting from './Waiting.svelte';
	import AutoStartNote from './AutoStartNote.svelte';
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { claimAutoStart, releaseAutoStart, shouldAutoStart } from '#lib/planning/autoStart.js';
	import ErrorNote from './ErrorNote.svelte';

	let {
		project,
		changed,
		back,
		go
	}: {
		project: Project;
		changed: () => Promise<void>;
		back: string;
		go: (step: StepId) => void;
	} = $props();

	let brief = $state<Brief | null>(null);
	let questions = $state<Question[]>([]);
	let draft = $state<Draft>({});
	const asking = createAction();
	const saving = createAction();
	const loading = createAction();
	let editing = $state<{ id: string | null; state: EditorState } | null>(null);
	const editor = createAction();
	const adjustable = $derived(project.status === 'questions_ready');
	const open = $derived(rank(project.status) <= 2);
	/** Questions written for an earlier wish: the wish changed, so they must be asked again. */
	const stale = $derived(project.status === 'draft' && questions.length > 0);
	const none = $derived(questions.length === 0 || stale);
	const unanswered = $derived(questions.filter((q) => firstIncomplete([q], draft)).length);

	async function load() {
		await loading.run(async () => {
			[brief, questions] = await Promise.all([getBrief(project.id), getQuestions(project.id)]);
			draft = initialDraft(questions);
			didLoad = true;
		});
	}

	async function ask() {
		const ok = await asking.run(async () => {
			if (!brief || project.status === 'draft') brief = await generateBrief(project.id);
			questions = await generateQuestions(project.id);
			draft = initialDraft(questions);
		});
		if (ok) {
			releaseAutoStart(project.id, 'questions');
			await changed();
		}
	}

	const keyMissing = $derived(engineKey.keys !== null && engineKey.keys.planning === null);
	let didLoad = $state(false);

	$effect(() => {
		const go = shouldAutoStart({
			loaded: didLoad,
			missingContent: none,
			stateAllows: project.status === 'draft',
			busy: asking.busy || loading.busy,
			failed: asking.error !== null,
			keyReady: engineKey.keys === null ? null : engineKey.keys.planning !== null
		});
		if (go && claimAutoStart(project.id, 'questions')) void ask();
	});

	/** Takes the list the server returned; answers typed for untouched questions are kept. */
	function adopt(next: Question[], touched: string | null) {
		const keep = { ...draft };
		if (touched) delete keep[touched];
		questions = next;
		draft = {
			...initialDraft(next),
			...Object.fromEntries(Object.entries(keep).filter(([id]) => next.some((q) => q.id === id)))
		};
	}

	async function saveEditor() {
		if (!editing) return;
		const { id, state } = editing;
		const ok = await editor.run(async () => {
			const input = toInput(state);
			adopt(
				id ? await editQuestion(project.id, id, input) : await addQuestion(project.id, input),
				id
			);
		});
		if (ok) editing = null;
	}

	let removing = $state<Question | null>(null);

	async function remove(id: string) {
		const ok = await editor.run(async () => adopt(await deleteQuestion(project.id, id), id));
		removing = null;
		if (ok) toasts.show(locale.t('toast.questionRemoved'));
	}

	async function save() {
		const missing = firstIncomplete(questions, draft);
		if (missing) {
			saving.fail(locale.t('questions.incomplete', { seq: String(missing.seq) }));
			return;
		}
		const ok = await saving.run(async () => {
			questions = await saveAnswers(project.id, toRequest(questions, draft));
			draft = initialDraft(questions);
		});
		if (ok) {
			await changed();
			toasts.show(locale.t('questions.saved'));
			go('requirements');
		}
	}

	$effect(() => {
		void project.id;
		void project.status;
		void load();
	});
</script>

<StepShell
	step="questions"
	heading={locale.t('questions.heading')}
	lead={locale.t('step.questionsLead')}
	{back}
	reason={open && !none && unanswered > 0
		? locale.t('questions.progress', {
				done: String(questions.length - unanswered),
				total: String(questions.length)
			})
		: null}
>
	{#if loading.error}<p role="alert">{loading.error}</p>{/if}

	{#if brief}
		<details class="brief" data-testid="brief">
			<summary>{locale.t('brief.heading')}</summary>
			<dl>
				<dt>{locale.t('brief.goal')}</dt>
				<dd>{brief.goal}</dd>
				<dt>{locale.t('brief.actors')}</dt>
				<dd>{brief.actors.join(', ')}</dd>
				<dt>{locale.t('brief.terms')}</dt>
				<dd>{brief.domainTerms.join(', ')}</dd>
				<dt>{locale.t('brief.risks')}</dt>
				<dd>{brief.risks.join('; ')}</dd>
			</dl>
		</details>
	{/if}

	{#if loading.busy && questions.length === 0}
		<div class="skeleton" aria-hidden="true"></div>
		<div class="skeleton" aria-hidden="true"></div>
	{:else if none}
		<div class="empty">
			<h3>{locale.t('questions.emptyTitle')}</h3>
			<p>{locale.t(stale ? 'questions.stale' : 'questions.emptyStep')}</p>
		</div>
		{#if asking.busy}<Waiting what={locale.t('questions.asking')} />{/if}
		<AutoStartNote
			error={asking.error}
			keyIssue={asking.keyIssue}
			{keyMissing}
			busy={asking.busy}
			retry={() => void ask()}
			testid="ask-error"
		/>
	{:else}
		<p class="sub">{locale.t(open ? 'questions.intro' : 'questions.done')}</p>
		<form
			id="answers-form"
			onsubmit={(event) => {
				event.preventDefault();
				void save();
			}}
		>
			{#each questions as question (question.id)}
				{@const value = draft[question.id] ?? question.proposal}
				<fieldset disabled={!open} data-testid={`question-${question.seq}`}>
					<legend>{question.seq}. {question.text}</legend>
					<div class="options">
						{#if question.answerType === 'single_choice'}
							{#each question.options as option (option)}
								<label class="choice" class:picked={value === option}>
									<input
										type="radio"
										name={question.id}
										value={option}
										checked={value === option}
										onchange={() => (draft[question.id] = option)}
									/>
									{option}
									{#if asText(question.proposal) === option}
										<span class="badge">{locale.t('questions.aiBadge')}</span>
									{/if}
								</label>
							{/each}
						{:else if question.answerType === 'multiple_choice'}
							{#each question.options as option (option)}
								<label class="choice" class:picked={Array.isArray(value) && value.includes(option)}>
									<input
										type="checkbox"
										checked={Array.isArray(value) && value.includes(option)}
										onchange={(e) =>
											(draft[question.id] = toggle(value, option, e.currentTarget.checked))}
									/>
									{option}
									{#if Array.isArray(question.proposal) && question.proposal.includes(option)}
										<span class="badge">{locale.t('questions.aiBadge')}</span>
									{/if}
								</label>
							{/each}
						{:else}
							<textarea
								rows="2"
								value={typeof value === 'string' ? value : ''}
								oninput={(e) => (draft[question.id] = e.currentTarget.value)}></textarea>
						{/if}
					</div>
					<small>
						{locale.t('questions.proposal', { proposal: asText(question.proposal) })}
						{#if question.explanation}{question.explanation}{/if}
						{#if question.answer}
							<em data-testid={`decided-${question.seq}`}>
								{locale.t(
									question.answer.decidedBy === 'ai' ? 'questions.byAi' : 'questions.byClient'
								)}
							</em>
						{/if}
					</small>
					{#if adjustable}
						<div class="row-actions">
							<button
								type="button"
								onclick={() => (editing = { id: question.id, state: editorFrom(question) })}
								data-testid={`edit-${question.seq}`}
							>
								{locale.t('questions.edit')}
							</button>
							<button
								type="button"
								class="danger"
								onclick={() => (removing = question)}
								data-testid={`remove-${question.seq}`}
							>
								{locale.t('list.delete')}
							</button>
						</div>
					{/if}
				</fieldset>
			{/each}
			{#if editor.error && !editing}<p role="alert">{editor.error}</p>{/if}
			{#if adjustable && !editing}
				<div class="optional">
					<span class="optional-label">{locale.t('step.optional')}</span>
					<button
						type="button"
						class="quiet"
						onclick={() => (editing = { id: null, state: emptyEditor() })}
						disabled={questions.length >= 8}
						data-testid="add-question"
					>
						{locale.t('questions.add')}
					</button>
					<button
						type="button"
						class="quiet"
						onclick={ask}
						disabled={asking.busy}
						data-testid="ask-again"
					>
						{asking.busy ? locale.t('questions.asking') : locale.t('questions.askAgain')}
					</button>
				</div>
			{/if}
			{#if asking.busy}<Waiting what={locale.t('questions.asking')} />{/if}
			{#if asking.error}<ErrorNote message={asking.error} keyIssue={asking.keyIssue} />{/if}

			{#if saving.error}<p role="alert" data-testid="save-error">{saving.error}</p>{/if}
		</form>
		{#if editing}
			<QuestionEditor
				state={editing.state}
				busy={editor.busy}
				error={editor.error}
				save={saveEditor}
				cancel={() => (editing = null)}
			/>
		{/if}
	{/if}

	{#snippet primary()}
		{#if none && project.status === 'draft'}
			<button class="primary" disabled data-testid="save-answers">
				{locale.t('questions.save')}
			</button>
		{:else if none}
			<button
				class="primary"
				onclick={ask}
				disabled={asking.busy || loading.busy}
				data-testid="ask"
			>
				{asking.busy ? locale.t('questions.asking') : locale.t('questions.ask')}
			</button>
		{:else if open}
			<button
				type="submit"
				form="answers-form"
				class="primary"
				disabled={saving.busy || unanswered > 0}
				data-testid="save-answers"
			>
				{saving.busy ? locale.t('questions.saving') : locale.t('questions.save')}
			</button>
		{:else}
			<a class="button primary" href={stepHash(project.id, 'requirements')}>
				{locale.t('step.continueRequirements')}
			</a>
		{/if}
	{/snippet}
</StepShell>

<ConfirmDialog
	open={removing !== null}
	title={locale.t('questions.removeTitle')}
	text={locale.t('questions.removeText')}
	confirmLabel={locale.t('list.delete')}
	busy={editor.busy}
	testid="remove-question"
	onconfirm={() => removing && remove(removing.id)}
	oncancel={() => (removing = null)}
/>

<style>
	.brief {
		border: 1px solid var(--line);
		border-radius: 10px;
		padding: 0.6rem 1rem;
		margin-bottom: 1.25rem;
		background: var(--sunken);
	}
	.brief summary {
		font-weight: 600;
		cursor: pointer;
	}
	.brief dl {
		display: grid;
		grid-template-columns: 8rem 1fr;
		gap: 0.3rem 1rem;
		margin: 0.75rem 0 0.25rem;
	}
	.brief dt {
		color: var(--muted);
	}
	.brief dd {
		margin: 0;
	}
	fieldset {
		margin: 0 0 1rem;
		padding: 0.9rem 1.25rem 1rem;
	}
	legend {
		font-size: 1.02rem;
	}
	.options {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
	}
	.choice {
		display: inline-flex;
		align-items: center;
		gap: 0.5rem;
		margin: 0;
		padding: 0.45rem 0.85rem;
		border: 1px solid var(--field-line);
		border-radius: 10px;
		font-weight: 400;
		cursor: pointer;
	}
	.choice.picked {
		background: var(--blue-soft);
		border-color: var(--blue);
	}
	.choice:has(:focus-visible) {
		outline: 2px solid var(--blue);
		outline-offset: 2px;
	}
	textarea {
		width: 100%;
	}
	small {
		display: block;
		color: var(--muted);
		margin-top: 0.6rem;
	}
	.row-actions {
		display: flex;
		gap: 0.5rem;
		margin-top: 0.6rem;
	}
	.row-actions button {
		padding: 0.2rem 0.7rem;
		font-size: 0.9rem;
	}
</style>
