<script lang="ts">
	import { engineKey } from '#lib/ai/key.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';

	/** An inline error near an action; offers the key panel when a new key would fix it. */
	let {
		message,
		keyIssue = false,
		testid
	}: { message: string; keyIssue?: boolean; testid?: string } = $props();
</script>

<p role="alert" data-testid={testid}>
	{message}
	{locale.t('error.next')}
	{#if keyIssue}
		<button type="button" onclick={() => engineKey.openPanel()} data-testid="open-key-panel">
			{locale.t('key.enter')}
		</button>
	{/if}
</p>
