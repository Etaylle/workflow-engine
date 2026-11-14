export type Health = { status: string; app: string };

/** Turns the answer of GET /api/health into the text shown on the page. */
export function healthText(health: Health): string {
	return health.status === 'UP' ? `${health.app} is up` : `${health.app} is down`;
}
