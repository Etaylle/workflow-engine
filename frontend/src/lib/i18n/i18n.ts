import de from './messages/de.json';
import en from './messages/en.json';

export const LOCALES = ['en', 'de'] as const;
export type Locale = (typeof LOCALES)[number];
export type MessageKey = keyof typeof en;

const catalogs: Record<Locale, Record<string, string>> = { en, de };

export function isLocale(value: string | null | undefined): value is Locale {
	return LOCALES.some((locale) => locale === value);
}

/** Looks a message up and fills {name} placeholders; a missing key is an error, not a blank. */
export function translate(
	locale: Locale,
	key: MessageKey,
	params: Record<string, string> = {}
): string {
	const template = catalogs[locale][key];
	if (template === undefined) {
		throw new Error(`Message "${key}" is missing in the ${locale} messages.`);
	}
	return template.replace(/\{(\w+)\}/g, (placeholder, name: string) => params[name] ?? placeholder);
}

/** True when the key exists (backend-supplied keys are checked before use). */
export function hasMessage(key: string): boolean {
	return key in en;
}
