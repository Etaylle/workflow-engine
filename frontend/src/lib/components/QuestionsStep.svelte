<script lang="ts">
	import {
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

	let { project, changed }: { project: Project; changed: () => void } = $props();

	let brief = $state<Brief | null>(null);
	let questions = $state<Question[]>([]);
	let draft = $state<Draft>({});
	const asking = createAction();
	const saving = createAction();
	const loading = createAction();
	const open = $derived(rank(project.status) <= 2);

	async function load() {
		await loading.run(async () => {
			[brief, questions] = await Promise.all([getBrief(project.id), getQuestions(project.id)]);
			draft = initialDraft(questions);
		});
	}

	async function ask() {
		const ok = await asking.run(async () => {
			if (!brief) brief = await generateBrief(project.id);
			questions = await generateQuestions(project.id);
			draft = initialDraft(questions);
		});
		if (ok) changed();
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

	{#if questions.length === 0}
		<p>{locale.t('questions.none')}</p>
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
			{#if open}
				<button type="submit" disabled={saving.busy} data-testid="save-answers">
					{saving.busy ? locale.t('questions.saving') : locale.t('questions.save')}
				</button>
				{#if project.status === 'answered'}
					<p role="status" data-testid="answers-saved">{locale.t('questions.saved')}</p>
				{/if}
			{/if}
			{#if saving.error}<p role="alert" data-testid="save-error">{saving.error}</p>{/if}
		</form>
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
