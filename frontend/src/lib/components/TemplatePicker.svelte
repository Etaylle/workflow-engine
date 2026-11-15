<script lang="ts">
	import { listTemplates, updateProject, type Project, type Template } from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { templateOptions } from '#lib/planning/templates.js';
	import { toasts } from '#lib/shell/toast.svelte.js';

	/** The kind of software to build: proposed from the wish, changeable until the run starts. */
	let { project }: { project: Project } = $props();

	let options = $state<Template[]>([]);
	let chosen = $state('');
	const saving = createAction();

	$effect(() => {
		chosen = project.templateId;
		void listTemplates().then(
			(all) => (options = templateOptions(all)),
			() => saving.fail(locale.t('error.unreachable'))
		);
	});

	function label(template: Template): string {
		const key = `template.${template.id}` as MessageKey;
		const text = locale.t(key);
		return text === key ? template.name : text;
	}

	async function choose() {
		const ok = await saving.run(() => updateProject(project.id, { templateId: chosen }));
		if (ok) toasts.show(locale.t('template.savedToast'));
	}
</script>

<select
	bind:value={chosen}
	onchange={() => void choose()}
	aria-label={locale.t('review.technology')}
	data-testid="template-select"
>
	{#each options as option (option.id)}
		<option value={option.id}>{label(option)}</option>
	{/each}
</select>
{#if saving.error}<p role="alert">{saving.error}</p>{/if}
