<script lang="ts">
	import { ApiError, deleteProject, renameProject, type Project } from '#lib/api/client.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { relativeTime } from '#lib/shell/relativeTime.js';
	import { statusKey, statusTone } from '#lib/shell/status.js';

	let {
		projects,
		failed,
		changed
	}: { projects: Project[]; failed: boolean; changed: () => Promise<void> } = $props();

	let renaming = $state<{ id: string; name: string } | null>(null);
	let deleting = $state<string | null>(null);
	let error = $state<string | null>(null);

	async function act(action: () => Promise<unknown>) {
		error = null;
		try {
			await action();
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
												void act(() => renameProject(project.id, renaming?.name ?? ''));
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
									<div class="actions">
										{#if deleting === project.id}
											<span>{locale.t('list.confirmDelete', { name: project.name })}</span>
											<button
												class="danger"
												onclick={() => act(() => deleteProject(project.id))}
												data-testid="confirm-delete"
											>
												{locale.t('list.delete')}
											</button>
											<button onclick={() => (deleting = null)}>{locale.t('list.cancel')}</button>
										{:else}
											<a class="button" href={`#${project.id}`}>{locale.t('projects.open')}</a>
											<button
												onclick={() => (renaming = { id: project.id, name: project.name })}
												data-testid="rename"
											>
												{locale.t('list.rename')}
											</button>
											<button onclick={() => (deleting = project.id)} data-testid="delete">
												{locale.t('list.delete')}
											</button>
										{/if}
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

<style>
	.head {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		align-items: flex-start;
		gap: 1rem;
	}
	.button {
		display: inline-block;
		padding: 0.45rem 1rem;
		border: 1px solid #c9c5bb;
		border-radius: 8px;
		background: var(--surface);
		color: var(--ink);
		text-decoration: none;
	}
	.button.primary {
		background: var(--blue);
		border-color: var(--blue);
		color: #fff;
		padding: 0.7rem 1.4rem;
	}
	.scroll {
		overflow-x: auto;
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
	.actions {
		white-space: nowrap;
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		align-items: center;
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
