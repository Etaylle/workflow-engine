import type { MessageKey } from '#lib/i18n/i18n.js';

export type PresetId = 'gemini' | 'openai' | 'groq' | 'openrouter' | 'custom';

export type Preset = {
	id: PresetId;
	/** Name of the provider in the key field label and in the header. */
	name: string;
	label: MessageKey;
	hint: MessageKey;
	baseUrl: string;
	model: string;
};

/** Example endpoints of OpenAI-compatible providers; the model ids are examples, not promises. */
export const PRESETS: Preset[] = [
	{
		id: 'gemini',
		name: 'Gemini',
		label: 'key.presetGemini',
		hint: 'key.hintGemini',
		baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai/',
		model: 'gemini-flash-lite-latest'
	},
	{
		id: 'openai',
		name: 'OpenAI',
		label: 'key.presetOpenai',
		hint: 'key.hintOpenai',
		baseUrl: 'https://api.openai.com/v1',
		model: 'gpt-5.4-mini'
	},
	{
		id: 'groq',
		name: 'Groq',
		label: 'key.presetGroq',
		hint: 'key.hintGroq',
		baseUrl: 'https://api.groq.com/openai/v1',
		model: 'llama-3.3-70b-versatile'
	},
	{
		id: 'openrouter',
		name: 'OpenRouter',
		label: 'key.presetOpenrouter',
		hint: 'key.hintOpenrouter',
		baseUrl: 'https://openrouter.ai/api/v1',
		model: 'openai/gpt-4o-mini'
	},
	{
		id: 'custom',
		name: '',
		label: 'key.presetCustom',
		hint: 'key.hintCustom',
		baseUrl: '',
		model: ''
	}
];

export const DEFAULT_PRESET = PRESETS[0] as Preset;

/** The preset a base URL belongs to; any other URL is "custom". */
export function presetOfUrl(baseUrl: string | null | undefined): Preset {
	const url = (baseUrl ?? '').trim().replace(/\/+$/, '');
	return (
		PRESETS.find((p) => p.baseUrl !== '' && p.baseUrl.replace(/\/+$/, '') === url) ??
		(PRESETS[PRESETS.length - 1] as Preset)
	);
}
