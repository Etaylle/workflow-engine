<script lang="ts">
	import { locale } from '#lib/i18n/state.svelte.js';
	import type { EditorState } from '#lib/planning/questionEditor.js';

	let {
		state,
		busy,
		error,
		save,
		cancel
	}: {
		state: EditorState;
		busy: boolean;
		error: string | null;
		save: () => void;
		cancel: () => void;
	} = $props();
</script>

<form
	class="card"
	onsubmit={(e) => {
		e.preventDefault();
		save();
	}}
	data-testid="question-editor"
>
	<label>
		{locale.t('questions.editText')}
		<textarea rows="2" bind:value={state.text} data-testid="editor-text"></textarea>
	</label>
	<label>
		{locale.t('questions.editType')}
		<select bind:value={state.answerType} data-testid="editor-type">
			<option value="single_choice">{locale.t('questions.typeSingle')}</option>
			<option value="multiple_choice">{locale.t('questions.typeMultiple')}</option>
			<option value="text">{locale.t('questions.typeText')}</option>
		</select>
	</label>
	<label>
		{locale.t('questions.editCategory')}
		<select bind:value={state.category}>
			<option value="requirement">{locale.t('questions.categoryRequirement')}</option>
			<option value="feature">{locale.t('questions.categoryFeature')}</option>
		</select>
	</label>
	{#if state.answerType !== 'text'}
		<label>
			{locale.t('questions.editOptions')}
			<textarea rows="4" bind:value={state.options} data-testid="editor-options"></textarea>
		</label>
	{/if}
	<label>
		{locale.t('questions.editProposal')}
		<textarea rows="2" bind:value={state.proposal} data-testid="editor-proposal"></textarea>
	</label>
	<button type="submit" class="primary" disabled={busy} data-testid="editor-save">
		{locale.t('list.save')}
	</button>
	<button type="button" onclick={cancel}>{locale.t('list.cancel')}</button>
	{#if error}<p role="alert" data-testid="editor-error">{error}</p>{/if}
</form>
