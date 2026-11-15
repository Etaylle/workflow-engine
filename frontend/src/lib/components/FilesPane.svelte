<script lang="ts">
	import {
		getRunFile,
		listRunCheckpoints,
		listRunFiles,
		type Checkpoint,
		type WorkspaceFile,
		type WorkspaceFileContent
	} from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import { buildTree, parentsOf, sizeLabel, visibleRows } from '#lib/run/files.js';

	/** What was built: checkpoints with their changed files, all files, and the text of one file. */
	let { runId, version }: { runId: string; version: number } = $props();

	let checkpoints = $state<Checkpoint[]>([]);
	let all = $state<WorkspaceFile[]>([]);
	let ref = $state('HEAD');
	let mode = $state<'changes' | 'all'>('changes');
	let expanded = $state<string[]>([]);
	let selected = $state<string | null>(null);
	let content = $state<WorkspaceFileContent | null>(null);
	const loading = createAction();
	const opening = createAction();

	const rows = $derived(visibleRows(buildTree(all), expanded));
	const shown = $derived(checkpoints.find((c) => c.sha === ref) ?? checkpoints[0] ?? null);

	async function load() {
		await loading.run(async () => {
			checkpoints = await listRunCheckpoints(runId);
			all = await listRunFiles(runId, ref);
		});
	}

	async function open(path: string) {
		selected = path;
		await opening.run(async () => {
			content = await getRunFile(runId, path, ref);
		});
	}

	function toggle(path: string) {
		expanded = expanded.includes(path) ? expanded.filter((p) => p !== path) : [...expanded, path];
	}

	async function pick(sha: string) {
		ref = sha;
		selected = null;
		content = null;
		await load();
	}

	function reveal(path: string) {
		mode = 'all';
		expanded = [...new Set([...expanded, ...parentsOf(path)])];
		void open(path);
	}

	$effect(() => {
		void runId;
		void version;
		void load();
	});
</script>

<div class="files" data-testid="files">
	<div class="nav">
		<div class="switch" role="group" aria-label={locale.t('files.view')}>
			<button
				type="button"
				class:current={mode === 'changes'}
				aria-pressed={mode === 'changes'}
				onclick={() => (mode = 'changes')}
				data-testid="files-mode-changes"
			>
				{locale.t('files.changes')}
			</button>
			<button
				type="button"
				class:current={mode === 'all'}
				aria-pressed={mode === 'all'}
				onclick={() => (mode = 'all')}
				data-testid="files-mode-all"
			>
				{locale.t('files.all')}
			</button>
		</div>
		{#if loading.error}<p role="alert">{loading.error}</p>{/if}
		{#if mode === 'changes'}
			<label class="pick">
				{locale.t('files.checkpoint')}
				<select value={ref} onchange={(e) => pick(e.currentTarget.value)} data-testid="files-ref">
					<option value="HEAD">{locale.t('files.latest')}</option>
					{#each checkpoints as checkpoint (checkpoint.sha)}
						<option value={checkpoint.sha}>{checkpoint.message}</option>
					{/each}
				</select>
			</label>
			{#if shown}
				<ul class="changes" data-testid="files-changes">
					{#each shown.files as change (change.path)}
						<li>
							<button
								type="button"
								class="file"
								class:current={selected === change.path}
								onclick={() => reveal(change.path)}
								data-testid="change-file"
							>
								<span class="name">{change.path}</span>
								<span class="delta">
									{#if change.binary}{locale.t('files.binary')}{:else}<span class="add"
											>+{change.added}</span
										>
										<span class="del">-{change.removed}</span>{/if}
								</span>
							</button>
						</li>
					{/each}
				</ul>
				{#if shown.fileCount > shown.files.length}
					<p class="more">
						{locale.t('files.more', { count: String(shown.fileCount - shown.files.length) })}
					</p>
				{/if}
			{/if}
		{:else}
			<ul class="tree" data-testid="files-tree">
				{#each rows as row (row.node.path)}
					<li style:padding-left={`${row.depth * 0.9}rem`}>
						{#if row.node.dir}
							<button
								type="button"
								class="dir"
								aria-expanded={expanded.includes(row.node.path)}
								onclick={() => toggle(row.node.path)}
							>
								{expanded.includes(row.node.path) ? '▾' : '▸'}
								{row.node.name}
							</button>
						{:else}
							<button
								type="button"
								class="file"
								class:current={selected === row.node.path}
								onclick={() => open(row.node.path)}
								data-testid="tree-file"
							>
								<span class="name">{row.node.name}</span>
								<span class="delta">{sizeLabel(row.node.size)}</span>
							</button>
						{/if}
					</li>
				{/each}
			</ul>
		{/if}
	</div>
	<div class="viewer" data-testid="file-view">
		{#if opening.error}
			<p role="alert">{opening.error}</p>
		{:else if !content || selected === null}
			<p class="empty">{locale.t('files.choose')}</p>
		{:else}
			<p class="path">
				{content.path} · {sizeLabel(content.size)}
			</p>
			{#if content.binary}
				<p class="empty">{locale.t('files.binaryNote')}</p>
			{:else if content.tooLarge}
				<p class="empty">{locale.t('files.tooLarge', { size: sizeLabel(content.size) })}</p>
			{:else}
				<pre class="code" tabindex="-1" data-testid="file-content">{content.content}</pre>
			{/if}
		{/if}
	</div>
</div>

<style>
	.files {
		display: grid;
		grid-template-columns: minmax(14rem, 22rem) minmax(0, 1fr);
		gap: var(--space-4);
		flex: 1 1 auto;
		min-height: 0;
	}
	.nav,
	.viewer {
		min-height: 0;
		overflow: auto;
		border: 1px solid var(--line);
		border-radius: var(--radius-sm);
		background: var(--sunken);
		padding: var(--space-3);
	}
	.switch {
		display: flex;
		gap: var(--space-1);
		margin-bottom: var(--space-3);
	}
	.switch button.current {
		background: var(--blue-soft);
		border-color: var(--blue-line);
		color: var(--blue-ink);
	}
	.pick {
		display: block;
		font-size: var(--text-sm);
		margin-bottom: var(--space-3);
	}
	.pick select {
		width: 100%;
	}
	ul {
		list-style: none;
		margin: 0;
		padding: 0;
	}
	.file,
	.dir {
		display: flex;
		justify-content: space-between;
		gap: var(--space-2);
		width: 100%;
		border: 0;
		background: transparent;
		text-align: left;
		padding: 0.15rem var(--space-2);
		min-height: 0;
		font-family: ui-monospace, 'SF Mono', Menlo, Consolas, monospace;
		font-size: var(--text-xs);
	}
	.file.current {
		background: var(--blue-soft);
		color: var(--blue-ink);
	}
	.delta {
		color: var(--muted);
		white-space: nowrap;
	}
	.add {
		color: var(--green-ink);
	}
	.del {
		color: var(--red);
	}
	.more,
	.empty,
	.path {
		color: var(--muted);
		font-size: var(--text-sm);
		margin: 0 0 var(--space-2);
	}
	.code {
		margin: 0;
		font-family: ui-monospace, 'SF Mono', Menlo, Consolas, monospace;
		font-size: var(--text-xs);
		line-height: 1.5;
		white-space: pre;
		overflow-x: auto;
	}
	@media (max-width: 56rem) {
		.files {
			grid-template-columns: minmax(0, 1fr);
		}
	}
</style>
