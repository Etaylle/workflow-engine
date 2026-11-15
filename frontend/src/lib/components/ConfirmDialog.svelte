<script lang="ts">
	import { locale } from '#lib/i18n/state.svelte.js';

	/** In-page confirmation in a native modal dialog: focus is trapped, Escape closes. */
	let {
		open,
		title,
		text,
		confirmLabel,
		busy = false,
		error = null,
		testid = 'confirm',
		onconfirm,
		oncancel
	}: {
		open: boolean;
		title: string;
		text: string;
		confirmLabel: string;
		busy?: boolean;
		error?: string | null;
		testid?: string;
		onconfirm: () => void;
		oncancel: () => void;
	} = $props();

	/** Opens the dialog modally on mount and closes it on removal so focus returns to the opener. */
	function modal(node: HTMLDialogElement) {
		node.showModal();
		return { destroy: () => node.close() };
	}
</script>

{#if open}
	<dialog
		use:modal
		class="confirm"
		aria-labelledby={`${testid}-title`}
		aria-describedby={`${testid}-text`}
		data-testid={testid}
		oncancel={(e) => {
			e.preventDefault();
			oncancel();
		}}
	>
		<h2 id={`${testid}-title`}>{title}</h2>
		<p id={`${testid}-text`}>{text}</p>
		{#if error}<p role="alert" data-testid={`${testid}-error`}>{error}</p>{/if}
		<div class="actions">
			<!-- svelte-ignore a11y_autofocus -->
			<button type="button" onclick={oncancel} autofocus data-testid={`${testid}-no`}>
				{locale.t('dialog.cancel')}
			</button>
			<button
				type="button"
				class="danger solid"
				onclick={onconfirm}
				disabled={busy}
				data-testid={`${testid}-yes`}
			>
				{confirmLabel}
			</button>
		</div>
	</dialog>
{/if}
