import { describe, expect, it } from 'vitest';
import type { AiKeys } from '#lib/api/client.js';
import { describeKey, isKeyProblem, keyWarning, providerName, typedKey } from './keyState.js';
import { PRESETS, presetOfUrl } from './presets.js';

const none: AiKeys = {
	anthropic: { set: false, last4: null },
	openai: { set: false, last4: null, baseUrl: null, model: null },
	planning: null,
	defaults: { baseUrl: 'https://api.openai.com/v1', model: 'gpt-5.4-mini' },
	saved: false,
	signIn: false
};
const gemini: AiKeys = {
	...none,
	openai: {
		set: true,
		last4: '1234',
		baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai/',
		model: 'gemini-flash-lite-latest'
	},
	planning: { provider: 'openai', model: 'gemini-flash-lite-latest' }
};
const name = (p: string) => (p === 'anthropic' ? 'Claude' : 'Gemini');

describe('describeKey', () => {
	it('says no key when none is set or the state is unknown', () => {
		expect(describeKey(none, name).key).toBe('key.noKey');
		expect(describeKey(null, name).key).toBe('key.noKey');
	});
	it('shows the active provider and only the last four characters', () => {
		expect(describeKey(gemini, name)).toEqual({
			key: 'key.keyEnding',
			params: { name: 'Gemini', last4: '1234' }
		});
	});
	it('shows no suffix for a short key', () => {
		const short = { ...gemini, openai: { ...gemini.openai, last4: '' } };
		expect(describeKey(short, name).key).toBe('key.keyShort');
	});
});

describe('providerName', () => {
	it('names a preset by its base URL and a custom endpoint by its host', () => {
		expect(providerName(gemini, 'openai')).toBe('Gemini');
		const custom = {
			...gemini,
			openai: { ...gemini.openai, baseUrl: 'https://llm.example.org/v1' }
		};
		expect(providerName(custom, 'openai')).toBe('llm.example.org');
	});
});

describe('presets', () => {
	it('puts Gemini first and finds a preset by URL, ignoring a trailing slash', () => {
		expect(PRESETS[0]?.id).toBe('gemini');
		expect(presetOfUrl('https://api.groq.com/openai/v1/').id).toBe('groq');
		expect(presetOfUrl('https://elsewhere.example/v1').id).toBe('custom');
	});
});

describe('isKeyProblem', () => {
	it('is true for a missing or rejected key only', () => {
		expect(isKeyProblem('error.keyMissing')).toBe(true);
		expect(isKeyProblem('error.keyRejected')).toBe(true);
		expect(isKeyProblem('error.rateLimit')).toBe(false);
	});
});

describe('typedKey', () => {
	it('trims and refuses blanks', () => {
		expect(typedKey('  sk-x ')).toBe('sk-x');
		expect(typedKey('   ')).toBeNull();
	});
});

describe('keyWarning', () => {
	it('warns when an Anthropic key sits in the compatible field and the reverse', () => {
		expect(keyWarning('openai', 'sk-ant-abc')).toBe('key.warnWrongSlotOpenai');
		expect(keyWarning('anthropic', 'AQ.abc')).toBe('key.warnWrongSlotAnthropic');
	});
	it('is quiet for a key in the right field and for an empty field', () => {
		expect(keyWarning('anthropic', 'sk-ant-abc')).toBeNull();
		expect(keyWarning('openai', 'AQ.abc')).toBeNull();
		expect(keyWarning('openai', '  ')).toBeNull();
	});
});
