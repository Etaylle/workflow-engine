<script lang="ts">
	import {
		generateRequirements,
		getRequirements,
		type Project,
		type Requirements
	} from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { rank } from '#lib/planning/steps.js';

	let { project, changed }: { project: Project; changed: () => void } = $props();

	const priorityKey = (priority: string) => `priority.${priority}` as MessageKey;

	let requirements = $state<Requirements>({ epics: [] });
	const writing = createAction();
	const loading = createAction();
	const written = $derived(requirements.epics.length > 0);
	const editable = $derived(rank(project.status) <= 3);

	async function load() {
		await loading.run(async () => (requirements = await getRequirements(project.id)));
	}

	async function write() {
		const ok = await writing.run(
			async () => (requirements = await generateRequirements(project.id))
		);
		if (ok) changed();
	}

	$effect(() => {
		void project.id;
		void project.status;
		void load();
	});
</script>

<section aria-labelledby="requirements-heading" data-testid="step-requirements">
	<h2 id="requirements-heading">{locale.t('requirements.heading')}</h2>
	{#if loading.error}<p role="alert">{loading.error}</p>{/if}
	{#if editable}
		<button onclick={write} disabled={writing.busy} data-testid="write-requirements">
			{writing.busy
				? locale.t('requirements.writing')
				: locale.t(written ? 'requirements.again' : 'requirements.write')}
		</button>
	{/if}
	{#if writing.error}<p role="alert" data-testid="requirements-error">{writing.error}</p>{/if}

	{#each requirements.epics as epic (epic.key)}
		<h3>{epic.key} {epic.name}</h3>
		<ul data-testid="requirements">
			{#each epic.requirements as requirement (requirement.key)}
				<li>
					<strong>{requirement.key}</strong>
					({locale.t(priorityKey(requirement.priority))}) {requirement.text}
				</li>
			{/each}
		</ul>
	{/each}
</section>
