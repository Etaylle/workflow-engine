export const MIN_LIMIT = 1;
export const MAX_LIMIT = 1000;

/** The cost limit typed by the client: empty means the default (null), a number from 1 to 1000 is kept. */
export function parseLimit(text: string): number | null | 'invalid' {
	const trimmed = text.trim().replace(',', '.');
	if (trimmed === '') return null;
	if (!/^\d+(\.\d{1,2})?$/.test(trimmed)) return 'invalid';
	const value = Number(trimmed);
	return value >= MIN_LIMIT && value <= MAX_LIMIT ? value : 'invalid';
}
