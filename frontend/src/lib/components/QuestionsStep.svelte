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
	import { rank } from '#lib/planning/steps.js';
	import {
		editorFrom,
		emptyEditor,
		toInput,
		type EditorState
	} from '#lib/planning/questionEditor.js';
	import QuestionEditor from './QuestionEditor.svelte';

	let { project, changed }: { project: Project; changed: () => void } = $props();

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

	async function load() {
		await loading.run(async () => {
			[brief, questions] = await Promise.all([getBrief(project.id), getQuestions(project.id)]);
			draft = initialDraft(questions);
		});
	}

	async function ask() {
		const ok = await asking.run(async () => {
			if (!brief || project.status === 'draft') brief = await generateBrief(project.id);
			questions = await generateQuestions(project.id);
			draft = initialDraft(questions);
		});
		if (ok) changed();
	}

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

	async function remove(id: string) {
		await editor.run(async () => adopt(await deleteQuestion(project.id, id), id));
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
		if (ok) changed();
	}

	$effect(() => {
		void project.id;
		void project.status;
		void load();
	});
</script>

<section aria-labelledby="questions-heading" data-testid="step-questions">
	<h2 id="questions-heading">{locale.t('questions.heading')}</h2>
	{#if loading.error}<p role="alert">{loading.error}</p>{/if}

	{#if brief}
		<details data-testid="brief">
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

	{#if questions.length === 0 || stale}
		<p>{locale.t(stale ? 'questions.stale' : 'questions.none')}</p>
		<button onclick={ask} disabled={asking.busy} data-testid="ask">
			{asking.busy ? locale.t('questions.asking') : locale.t('questions.ask')}
		</button>
		{#if asking.error}<p role="alert" data-testid="ask-error">{asking.error}</p>{/if}
	{:else}
		<p>{locale.t(open ? 'questions.intro' : 'questions.done')}</p>
		<form
			onsubmit={(event) => {
				event.preventDefault();
				void save();
			}}
		>
			{#each questions as question (question.id)}
				{@const value = draft[question.id] ?? question.proposal}
				<fieldset disabled={!open} data-testid={`question-${question.seq}`}>
					<legend>{question.seq}. {question.text}</legend>
					{#if question.answerType === 'single_choice'}
						{#each question.options as option (option)}
							<label class="choice">
								<input
									type="radio"
									name={question.id}
									value={option}
									checked={value === option}
									onchange={() => (draft[question.id] = option)}
								/>
								{option}
							</label>
						{/each}
					{:else if question.answerType === 'multiple_choice'}
						{#each question.options as option (option)}
							<label class="choice">
								<input
									type="checkbox"
									checked={Array.isArray(value) && value.includes(option)}
									onchange={(e) =>
										(draft[question.id] = toggle(value, option, e.currentTarget.checked))}
								/>
								{option}
							</label>
						{/each}
					{:else}
						<textarea
							rows="2"
							value={typeof value === 'string' ? value : ''}
							oninput={(e) => (draft[question.id] = e.currentTarget.value)}></textarea>
					{/if}
					{#if adjustable}
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
							onclick={() => remove(question.id)}
							data-testid={`remove-${question.seq}`}
						>
							{locale.t('list.delete')}
						</button>
					{/if}
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
				</fieldset>
			{/each}
			{#if adjustable && !editing}
				<button
					type="button"
					onclick={() => (editing = { id: null, state: emptyEditor() })}
					disabled={questions.length >= 8}
					data-testid="add-question"
				>
					{locale.t('questions.add')}
				</button>
				<button type="button" onclick={ask} disabled={asking.busy} data-testid="ask-again">
					{asking.busy ? locale.t('questions.asking') : locale.t('questions.askAgain')}
				</button>
			{/if}
			{#if editor.error && !editing}<p role="alert">{editor.error}</p>{/if}
			{#if open}
				<button type="submit" class="primary" disabled={saving.busy} data-testid="save-answers">
					{saving.busy ? locale.t('questions.saving') : locale.t('questions.save')}
				</button>
				{#if project.status === 'answered'}
					<p role="status" data-testid="answers-saved">{locale.t('questions.saved')}</p>
				{/if}
			{/if}
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
</section>

<style>
	fieldset {
		margin: 0.75rem 0;
	}
	.choice {
		display: block;
	}
	textarea {
		width: 100%;
		box-sizing: border-box;
	}
	small {
		display: block;
		color: #555;
		margin-top: 0.25rem;
	}
</style>
