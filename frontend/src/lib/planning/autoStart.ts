/** What the generation of a step needs to know before it may start by itself. */
export type AutoStartFacts = {
	/** The content of the step was read from the engine (an empty answer counts). */
	loaded: boolean;
	/** The content of the step does not exist yet. */
	missingContent: boolean;
	/** The project is in the state the generation expects. */
	stateAllows: boolean;
	/** A generation of this view is running. */
	busy: boolean;
	/** The last generation failed: the client decides with "Try again". */
	failed: boolean;
	/** Planning key held by the engine: null while unknown. */
	keyReady: boolean | null;
};

/** Whether arriving on a step starts its generation: only when everything is known and fine. */
export function shouldAutoStart(facts: AutoStartFacts): boolean {
	return (
		facts.loaded &&
		facts.missingContent &&
		facts.stateAllows &&
		!facts.busy &&
		!facts.failed &&
		facts.keyReady === true
	);
}

const started = new Set<string>();

/** Marks a generation as started for this tab; false when it was started before (no second start). */
export function claimAutoStart(projectId: string, step: string): boolean {
	const key = `${projectId}:${step}`;
	if (started.has(key)) return false;
	started.add(key);
	return true;
}

/** Frees the mark after a successful generation, so a later wish change can start it again. */
export function releaseAutoStart(projectId: string, step: string): void {
	started.delete(`${projectId}:${step}`);
}
