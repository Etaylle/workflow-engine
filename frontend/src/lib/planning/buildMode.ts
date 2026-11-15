export type BuildMode = 'whole' | 'tasks';

/** Software with at most this many software tasks is built all at once by default. */
export const WHOLE_MAX_TASKS = 8;

/** The mode a run uses: the client's choice, or whole for small software and tasks for larger. */
export function effectiveBuildMode(chosen: BuildMode | null, softwareTasks: number): BuildMode {
	return chosen ?? (softwareTasks <= WHOLE_MAX_TASKS ? 'whole' : 'tasks');
}
