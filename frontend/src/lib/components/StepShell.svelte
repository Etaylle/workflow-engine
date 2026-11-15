<script lang="ts">
	import { onMount, type Snippet } from 'svelte';
	import { locale } from '#lib/i18n/state.svelte.js';
	import type { StepId } from '#lib/planning/steps.js';

	/**
	 * One step of a project as its own view: heading with one sentence on what to do, the content,
	 * then a sticky footer with Back on the left and the one required action on the right.
	 */
	let {
		step,
		heading,
		lead,
		back,
		reason = null,
		children,
		primary
	}: {
		step: StepId;
		heading: string;
		lead: string;
		back: string;
		reason?: string | null;
		children: Snippet;
		primary: Snippet;
	} = $props();

	let title = $state<HTMLHeadingElement>();

	onMount(() => {
		window.scrollTo(0, 0);
		title?.focus({ preventScroll: true });
	});
</script>

<section class="step" aria-labelledby={`${step}-heading`} data-testid={`step-${step}`}>
	<h2 id={`${step}-heading`} tabindex="-1" bind:this={title}>{heading}</h2>
	<p class="sub">{lead}</p>
	{@render children()}
	<div class="step-footer">
		<a class="button" href={back} data-testid="step-back">{locale.t('step.back')}</a>
		{#if reason}<span class="reason" role="status" data-testid="step-reason">{reason}</span>{/if}
		{@render primary()}
	</div>
</section>

<style>
	h2:focus {
		outline: none;
	}
	.step {
		padding-bottom: 0;
	}
	.step-footer {
		position: sticky;
		bottom: 0;
		z-index: 5;
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: var(--space-3) var(--space-4);
		margin: var(--space-5) calc(-1 * clamp(var(--space-4), 3vw, var(--space-6))) 0;
		padding: var(--space-3) clamp(var(--space-4), 3vw, var(--space-6));
		background: var(--surface);
		border-top: 1px solid var(--line);
		border-radius: 0 0 var(--radius) var(--radius);
	}
	.reason {
		margin-left: auto;
		color: var(--amber-ink);
		font-size: var(--text-sm);
		font-weight: 500;
	}
	@media (max-width: 40rem) {
		.step-footer :global(.primary) {
			flex: 1 1 100%;
		}
	}
</style>
