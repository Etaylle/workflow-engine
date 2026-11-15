<script lang="ts">
	import { ApiError, deleteProject, updateProject, type Project } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { relativeTime } from '#lib/shell/relativeTime.js';
	import { statusKey, statusTone } from '#lib/shell/status.js';
	import { toasts } from '#lib/shell/toast.svelte.js';
	import ConfirmDialog from '#lib/components/ConfirmDialog.svelte';

	let {
		projects,
		failed,
		changed
	}: { projects: Project[]; failed: boolean; changed: () => Promise<void> } = $props();

	let renaming = $state<{ id: string; name: string } | null>(null);
	let deleting = $state<Project | null>(null);
	let error = $state<string | null>(null);

	async function act(action: () => Promise<unknown>, done: string) {
		error = null;
		try {
			await action();
			toasts.show(done);
			renaming = null;
			deleting = null;
			await changed();
		} catch (e) {
			error = e instanceof ApiError ? e.message : locale.t('error.unreachable');
		}
	}

	const snippet = (wish: string) => (wish.length > 110 ? `${wish.slice(0, 109)}…` : wish);
</script>

<div class="page">
	<div class="head">
		<div>
			<h1>{locale.t('list.title')}</h1>
			<p class="lead">{locale.t('list.lead')}</p>
		</div>
		<a class="button primary" href="#new" data-testid="new-project">{locale.t('list.new')}</a>
	</div>
	<p class="notice">{locale.t('list.notice')}</p>

	<div class="card">
		{#if failed}
			<p role="alert">{locale.t('projects.loadFailed')}</p>
		{:else if projects.length === 0}
			<p>{locale.t('projects.empty')}</p>
		{:else}
			<div class="scroll">
				<table data-testid="projects">
					<thead>
						<tr>
							<th>{locale.t('list.colProject')}</th>
							<th>{locale.t('list.colStatus')}</th>
							<th>{locale.t('list.colChanged')}</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						{#each projects as project (project.id)}
							<tr data-testid="project-row">
								<td>
									{#if renaming?.id === project.id}
										<form
											class="inline"
											onsubmit={(e) => {
												e.preventDefault();
												void act(
													() => updateProject(project.id, { name: renaming?.name ?? '' }),
													locale.t('toast.renamed')
												);
											}}
										>
											<input
												type="text"
												aria-label={locale.t('list.renameLabel')}
												bind:value={renaming.name}
												data-testid="rename-input"
											/>
											<button type="submit" class="primary">{locale.t('list.save')}</button>
											<button type="button" onclick={() => (renaming = null)}>
												{locale.t('list.cancel')}
											</button>
										</form>
									{:else}
										<a class="name" href={`#${project.id}`}>{project.name}</a>
										<div class="wish">{snippet(project.wish)}</div>
									{/if}
								</td>
								<td>
									<span class={`pill ${statusTone(project.status)}`}>
										{locale.t(statusKey(project.status))}
									</span>
								</td>
								<td>{relativeTime(project.updatedAt, new Date(), locale.current)}</td>
								<td>
									<div class="row-actions">
										<a class="button" href={`#${project.id}`}>{locale.t('projects.open')}</a>
										<button
											onclick={() => (renaming = { id: project.id, name: project.name })}
											data-testid="rename"
										>
											{locale.t('list.rename')}
										</button>
										<button
											class="danger"
											onclick={() => (deleting = project)}
											data-testid="delete"
										>
											{locale.t('list.delete')}
										</button>
									</div>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
		{/if}
		{#if error}<p role="alert">{error}</p>{/if}
	</div>
</div>

<ConfirmDialog
	open={deleting !== null}
	title={locale.t('list.deleteTitle')}
	text={locale.t('list.confirmDelete', { name: deleting?.name ?? '' })}
	confirmLabel={locale.t('list.delete')}
	testid="confirm-delete-dialog"
	onconfirm={() => deleting && act(() => deleteProject(deleting!.id), locale.t('toast.deleted'))}
	oncancel={() => (deleting = null)}
/>

<style>
	.head {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		align-items: flex-start;
		gap: 1rem;
	}
	table {
		width: 100%;
		border-collapse: collapse;
	}
	th {
		text-align: left;
		font-weight: 400;
		color: var(--muted);
		padding: 0.5rem 0.75rem;
	}
	td {
		padding: 0.9rem 0.75rem;
		border-top: 1px solid var(--line);
		vertical-align: middle;
	}
	.name {
		font-weight: 600;
		font-size: 1.1rem;
	}
	.wish {
		color: var(--muted);
	}
	.row-actions {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-2);
		justify-content: flex-end;
	}
	.inline {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
	}
	.inline input {
		flex: 1 1 14rem;
	}
</style>
