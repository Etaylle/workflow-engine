<script lang="ts">
	import {
		documentUrl,
		getDocument,
		type DocumentKind,
		type Project,
		type ProjectDocument
	} from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import type { MessageKey } from '#lib/i18n/i18n.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { rank } from '#lib/planning/steps.js';

	let { project }: { project: Project } = $props();

	const kinds = $derived<DocumentKind[]>(rank(project.status) >= 3 ? ['requirements', 'plan'] : []);
	let shown = $state<{ kind: DocumentKind; doc: ProjectDocument } | null>(null);
	const loading = createAction();

	async function view(kind: DocumentKind) {
		await loading.run(async () => {
			shown = { kind, doc: await getDocument(project.id, kind) };
		});
	}

	$effect(() => {
		void project.id;
		void project.status;
		shown = null;
	});
</script>

{#if kinds.length > 0}
	<section data-testid="documents">
		<h2>{locale.t('documents.heading')}</h2>
		<ul class="docs">
			{#each kinds as kind (kind)}
				<li class="card">
					<strong>{locale.t(`documents.${kind}` as MessageKey)}</strong>
					<button onclick={() => view(kind)} data-testid={`view-${kind}`}>
						{locale.t('documents.view')}
					</button>
					<a href={documentUrl(project.id, kind, 'docx')} data-testid={`docx-${kind}`}>DOCX</a>
					<a href={documentUrl(project.id, kind, 'md')}>Markdown</a>
				</li>
			{/each}
		</ul>
		{#if loading.error}<p role="alert">{loading.error}</p>{/if}
		{#if shown}
			<article class="card" data-testid="document-view">
				<h3>{shown.doc.title}</h3>
				{#each shown.doc.sections as section (section.heading)}
					<h4>{section.heading}</h4>
					{#each section.paragraphs as paragraph (paragraph)}<p>{paragraph}</p>{/each}
					{#if section.bullets.length > 0}
						<ul>
							{#each section.bullets as bullet (bullet)}<li>{bullet}</li>{/each}
						</ul>
					{/if}
				{/each}
			</article>
		{/if}
	</section>
{/if}

<style>
	.docs {
		list-style: none;
		padding: 0;
	}
	.docs li {
		display: flex;
		flex-wrap: wrap;
		gap: 1rem;
		align-items: center;
	}
</style>
