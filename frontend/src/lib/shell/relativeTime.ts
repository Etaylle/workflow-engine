const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
	['day', 86_400],
	['hour', 3_600],
	['minute', 60]
];

/** "2 minutes ago", "yesterday": the last change of a project in the language of the page. */
export function relativeTime(iso: string, now: Date, language: string): string {
	const seconds = Math.round((new Date(iso).getTime() - now.getTime()) / 1000);
	const format = new Intl.RelativeTimeFormat(language, { numeric: 'auto' });
	for (const [unit, size] of UNITS) {
		if (Math.abs(seconds) >= size) return format.format(Math.trunc(seconds / size), unit);
	}
	return format.format(0, 'second');
}
