import type { Preview } from '#lib/api/client.js';

/** There is a preview to show: under way, running, or ended with something to say. */
export const previewShown = (preview: Preview | null): boolean =>
	preview !== null && preview.status !== 'none';

/** The preview is building or running: it can be stopped and the stream stays open. */
export const previewActive = (preview: Preview | null): boolean =>
	preview !== null && (preview.status === 'starting' || preview.status === 'running');

/** The address of the running app on the machine the browser talks to the engine on. */
export const appUrl = (preview: Preview | null, hostname: string): string | null =>
	preview?.status === 'running' && preview.port ? `http://${hostname}:${preview.port}/` : null;

/** Whole minutes (rounded up) until a running preview ends; null when it has no end yet. */
export function minutesLeft(preview: Preview | null, now: number): number | null {
	if (preview?.status !== 'running' || !preview.expiresAt) return null;
	return Math.max(0, Math.ceil((Date.parse(preview.expiresAt) - now) / 60_000));
}
