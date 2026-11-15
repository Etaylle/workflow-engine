<script lang="ts">
	import { toasts } from '#lib/shell/toast.svelte.js';
</script>

<div class="toasts" role="status" aria-live="polite" data-testid="toasts">
	{#each toasts.items as toast (toast.id)}
		<div class="toast" data-testid="toast">{toast.text}</div>
	{/each}
</div>
