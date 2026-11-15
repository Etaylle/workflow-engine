<script lang="ts">
	import {
		copyStepProfile,
		deleteStepProfile,
		getProjectSettings,
		listStepProfiles,
		updateProjectSettings,
		type Project,
		type ProjectSettings,
		type StepProfile
	} from '#lib/api/client.js';
	import { createAction } from '#lib/api/action.svelte.js';
	import { locale } from '#lib/i18n/state.svelte.js';
	import ProfileEditor from './ProfileEditor.svelte';

	let { project }: { project: Project } = $props();

	let profiles = $state<StepProfile[]>([]);
	let settings = $state<ProjectSettings | null>(null);
	let editing = $state<string | null>(null);
	let copyOf = $state<string | null>(null);
	let copyName = $state('');
	let loadFailed = $state(false);
	const action = createAction();

	async function load() {
		try {
			[profiles, settings] = await Promise.all([
				listStepProfiles(),
				getProjectSettings(project.id)
			]);
			loadFailed = false;
		} catch {
			loadFailed = true;
		}
	}

	$effect(() => {
		void load();
	});

	async function choose(profile: StepProfile) {
		const ok = await action.run(async () => {
			settings = await updateProjectSettings(project.id, {
				stepProfileId: profile.isDefault ? null : profile.id,
				costLimitUsd: settings && !settings.usesDefaultLimit ? settings.costLimitUsd : null
			});
		});
		if (ok) await load();
	}

	async function copy(from: StepProfile) {
		const ok = await action.run(async () => {
			const created = await copyStepProfile(from.id, copyName);
			copyOf = null;
			copyName = '';
			editing = created.id;
		});
		if (ok) await load();
	}

	async function remove(profile: StepProfile) {
		if (await action.run(() => deleteStepProfile(profile.id))) await load();
	}
</script>

<main class="page" aria-labelledby="steps-heading">
	<a href={`#${project.id}`}>{locale.t('steps.back')}</a>
	<h1 id="steps-heading">{locale.t('steps.title')}</h1>
	<p class="lead">{locale.t('steps.lead')}</p>
	<p class="notice">{locale.t('steps.optional')}</p>
	{#if loadFailed}<p role="alert">{locale.t('error.unreachable')}</p>{/if}
	{#if action.error}<p role="alert">{action.error}</p>{/if}

	{#each profiles as profile (profile.id)}
		{@const used = settings?.stepProfileId === profile.id}
		<section class="card" data-testid="profile-card" data-profile={profile.name}>
			<h2>
				{profile.name}
				{#if used}<span class="pill done" data-testid="profile-used">{locale.t('steps.used')}</span
					>{/if}
				{#if profile.isDefault}<span class="pill">{locale.t('steps.default')}</span>{/if}
			</h2>
			<p class="lead">{profile.description}</p>
			{#if editing === profile.id}
				<ProfileEditor
					{profile}
					saved={() => {
						editing = null;
						void load();
					}}
					cancel={() => (editing = null)}
				/>
			{:else}
				<ol data-testid="profile-steps">
					{#each profile.steps as step (step.key)}
						<li>
							<strong>{step.title}</strong>
							<span class="lead">
								{step.gated ? locale.t('steps.checked') : locale.t('steps.notChecked')} · {locale.t(
									'steps.capValue',
									{ cap: step.capUsd.toFixed(2) }
								)}
							</span>
						</li>
					{/each}
				</ol>
				<button
					class="primary"
					disabled={used || action.busy}
					onclick={() => choose(profile)}
					data-testid="profile-use">{locale.t('steps.use')}</button
				>
				{#if profile.seeded}
					{#if copyOf === profile.id}
						<input
							bind:value={copyName}
							maxlength="60"
							placeholder={locale.t('steps.profileName')}
							data-testid="copy-name"
						/>
						<button
							disabled={action.busy || copyName.trim() === ''}
							onclick={() => copy(profile)}
							data-testid="copy-create">{locale.t('steps.copyCreate')}</button
						>
						<button onclick={() => (copyOf = null)}>{locale.t('list.cancel')}</button>
					{:else}
						<button onclick={() => (copyOf = profile.id)} data-testid="profile-copy"
							>{locale.t('steps.copy')}</button
						>
					{/if}
				{:else}
					<button onclick={() => (editing = profile.id)} data-testid="profile-edit"
						>{locale.t('steps.edit')}</button
					>
					<button
						disabled={action.busy}
						onclick={() => remove(profile)}
						data-testid="profile-delete">{locale.t('steps.delete')}</button
					>
				{/if}
			{/if}
		</section>
	{/each}
</main>
