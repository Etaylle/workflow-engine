<script lang="ts">
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	/** Under a step that writes itself: the failure with "Try again", or the missing planning key. */
	let {
		error,
		keyIssue = false,
		keyMissing,
		busy,
		retry,
		testid
	}: {
		error: string | null;
		keyIssue?: boolean;
		keyMissing: boolean;
		busy: boolean;
		retry: () => void;
		testid: string;
	} = $props();
</script>

{#if error}
	<p role="alert" data-testid={testid}>
		{error}
		<button type="button" onclick={retry} disabled={busy} data-testid="try-again">
			{locale.t('step.tryAgain')}
		</button>
		{#if keyIssue}
			<button type="button" onclick={() => engineKey.openPanel()} data-testid="open-key-panel">
				{locale.t('key.enter')}
			</button>
		{/if}
	</p>
{:else if keyMissing}
	<p role="status" data-testid="no-planning-key">
		{locale.t('step.noPlanningKey')}
		<button type="button" onclick={() => engineKey.openPanel()} data-testid="open-key-panel">
			{locale.t('key.enter')}
		</button>
	</p>
{/if}
