<script lang="ts">
	import { healthText, type Health } from './lib/health';

	let text = $state('checking');

	async function load(): Promise<void> {
		const response = await fetch('/api/health');
		text = healthText((await response.json()) as Health);
	}

	load().catch(() => {
		text = 'backend not reachable';
	});
</script>

<main>
	<h1>App</h1>
	<p data-testid="health">{text}</p>
</main>
