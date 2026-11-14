import { isLocale, translate, type Locale, type MessageKey } from './i18n.js';

const STORAGE_KEY = 'engine.locale';

function initialLocale(): Locale {
	const stored = localStorage.getItem(STORAGE_KEY);
	if (isLocale(stored)) return stored;
	return navigator.language.startsWith('de') ? 'de' : 'en';
}

class LocaleState {
	current = $state<Locale>('en');

	init(): void {
		this.current = initialLocale();
		document.documentElement.lang = this.current;
	}

	set(locale: Locale): void {
		this.current = locale;
		localStorage.setItem(STORAGE_KEY, locale);
		document.documentElement.lang = locale;
	}

	t = (key: MessageKey, params: Record<string, string> = {}): string =>
		translate(this.current, key, params);
}

export const locale = new LocaleState();
