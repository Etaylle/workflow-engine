<script lang="ts">
	let { data } = $props();
</script>

<svelte:head><title>App</title></svelte:head>

<h1>App</h1>
<p>{data.notes.length} notes</p>
<ul>
	{#each data.notes as note (note.id)}
		<li>{note.text}</li>
	{/each}
</ul>
