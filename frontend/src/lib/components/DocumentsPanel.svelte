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
	let open = $state(false);
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

	/** Opens the dialog modally on mount and closes it on removal so focus returns to the button. */
	function modal(node: HTMLDialogElement) {
		node.showModal();
		return { destroy: () => node.close() };
	}
</script>

{#if kinds.length > 0}
	<button type="button" class="quiet" onclick={() => (open = true)} data-testid="open-documents">
		{locale.t('documents.heading')}
	</button>
	{#if open}
		<dialog
			use:modal
			class="sheet"
			aria-labelledby="documents-title"
			data-testid="documents"
			oncancel={(e) => {
				e.preventDefault();
				open = false;
			}}
		>
			<div class="head">
				<h2 id="documents-title">{locale.t('documents.heading')}</h2>
				<!-- svelte-ignore a11y_autofocus -->
				<button
					type="button"
					autofocus
					onclick={() => (open = false)}
					data-testid="close-documents"
				>
					{locale.t('dialog.close')}
				</button>
			</div>
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
		</dialog>
	{/if}
{/if}

<style>
	.head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: var(--space-4);
		margin-bottom: var(--space-4);
	}
	.head h2 {
		margin: 0;
	}
	.docs {
		list-style: none;
		padding: 0;
		margin: 0;
		display: grid;
		gap: var(--space-3);
	}
	.docs li {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-3) var(--space-4);
		align-items: center;
		margin: 0;
	}
	.docs strong {
		margin-right: auto;
	}
</style>
