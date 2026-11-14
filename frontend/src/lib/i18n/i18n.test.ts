import { describe, expect, it } from 'vitest';
import de from './messages/de.json';
import en from './messages/en.json';
import { isLocale, translate, type MessageKey } from './i18n.js';

describe('translate', () => {
	it('returns the message of the locale', () => {
		expect(translate('en', 'list.title')).toBe('All projects');
		expect(translate('de', 'list.title')).toBe('Alle Projekte');
	});

	it('fills placeholders and keeps unknown ones visible', () => {
		expect(translate('en', 'status.footer', { name: 'x', build: 'stub' })).toContain('stub');
		expect(translate('en', 'status.footer')).toContain('{name}');
	});

	it('fails loudly for a missing key', () => {
		expect(() => translate('en', 'no.such.key' as MessageKey)).toThrow(/missing/);
	});

	it('has the same keys in English and German', () => {
		expect(Object.keys(de).sort()).toEqual(Object.keys(en).sort());
	});
});

describe('isLocale', () => {
	it('accepts only supported languages', () => {
		expect(isLocale('de')).toBe(true);
		expect(isLocale('fr')).toBe(false);
		expect(isLocale(null)).toBe(false);
	});
});
