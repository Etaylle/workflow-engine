import { describe, expect, it } from 'vitest';
import { normaliseBuilders } from '#lib/api/execution.js';
import {
	actionFor,
	builderOf,
	choiceAfterBuilderChange,
	isModelId,
	missingCodes,
	modelEffortText,
	modelToSave,
	otherAuthMode,
	startBlock
} from './builders.js';

const report = normaliseBuilders({
	demo: false,
	builders: [
		{
			id: 'claude_code',
			ready: false,
			authMode: 'machine_sign_in',
			missing: [
				{ code: 'not_signed_in', message: 'x', instructionKey: 'builders.claude.signInInstruction' }
			]
		},
		{ id: 'gemini_cli', ready: true, installed: true, missing: [] },
		{
			id: 'codex_cli',
			ready: false,
			installed: true,
			signedIn: 'no',
			efforts: ['low', 'medium', 'high', 'xhigh'],
			missing: [
				{
					code: 'codex_not_signed_in',
					message: 'x',
					instructionKey: 'builders.codex.signInInstruction'
				}
			]
		},
		{ id: 'other' as 'claude_code', ready: true }
	]
});

describe('builder report', () => {
	it('keeps known builders and tolerates absent fields', () => {
		expect(report.builders.map((b) => b.id)).toEqual(['claude_code', 'gemini_cli', 'codex_cli']);
		expect(builderOf(report, 'claude_code')?.missing[0]?.instructionKey).toBe(
			'builders.claude.signInInstruction'
		);
		expect(normaliseBuilders({}).builders).toEqual([]);
	});

	it('blocks the start only for a known builder that is not ready', () => {
		expect(startBlock(report, 'claude_code')?.id).toBe('claude_code');
		expect(missingCodes(startBlock(report, 'claude_code')!)).toEqual(['not_signed_in']);
		expect(startBlock(report, 'gemini_cli')).toBeNull();
		expect(startBlock(report, 'codex_cli')?.signedIn).toBe('no');
		expect(missingCodes(startBlock(report, 'codex_cli')!)).toEqual(['codex_not_signed_in']);
		expect(startBlock(null, 'claude_code')).toBeNull();
	});

	it('names an action for each missing thing', () => {
		expect(actionFor('no_anthropic_key')).toBe('open_key_panel');
		expect(actionFor('no_gemini_key')).toBe('open_key_panel');
		expect(actionFor('not_signed_in')).toBe('none');
		expect(actionFor('codex_not_signed_in')).toBe('none');
		expect(actionFor('surprise')).toBe('none');
	});

	it('offers the other Claude Code sign-in mode', () => {
		const claude = builderOf(report, 'claude_code')!;
		expect(otherAuthMode(claude)).toBe('anthropic_key');
		expect(otherAuthMode({ ...claude, authMode: 'anthropic_key' })).toBe('machine_sign_in');
	});
});

describe('builder model and effort', () => {
	it('reads the models and efforts of the report, tolerating absent lists', () => {
		const state = normaliseBuilders({
			builders: [
				{
					id: 'claude_code',
					models: [{ id: 'haiku', hint: 'cheap' }, { hint: 'no id' }],
					efforts: ['low', 'max']
				},
				{ id: 'gemini_cli', customModel: true },
				{ id: 'codex_cli', customModel: true, efforts: ['low', 'xhigh'] }
			]
		});
		expect(builderOf(state, 'claude_code')?.models).toEqual([{ id: 'haiku', hint: 'cheap' }]);
		expect(builderOf(state, 'claude_code')?.efforts).toEqual(['low', 'max']);
		expect(builderOf(state, 'gemini_cli')?.customModel).toBe(true);
		expect(builderOf(state, 'gemini_cli')?.efforts).toEqual([]);
		expect(builderOf(state, 'codex_cli')?.efforts).toEqual(['low', 'xhigh']);
	});

	it('accepts only model ids the API accepts', () => {
		expect(isModelId('gemini-3.1-flash-lite')).toBe(true);
		expect(isModelId('my_model.v2')).toBe(true);
		expect(isModelId('has space')).toBe(false);
		expect(isModelId('x'.repeat(81))).toBe(false);
		expect(modelToSave('  gemini-pro-latest ')).toBe('gemini-pro-latest');
		expect(modelToSave('  ')).toBe('');
		expect(modelToSave('bad id!')).toBeNull();
	});

	it('writes the choice as one line and leaves out what is not chosen', () => {
		expect(modelEffortText('fable', 'high')).toBe('fable · high');
		expect(modelEffortText('haiku', null)).toBe('haiku');
		expect(modelEffortText(null, undefined)).toBe('');
	});

	it('starts a new builder with the defaults and keeps the choice for the same builder', () => {
		const choice = { model: 'opus', effort: 'high' };
		expect(choiceAfterBuilderChange('claude_code', 'claude_code', choice)).toEqual(choice);
		expect(choiceAfterBuilderChange('claude_code', 'gemini_cli', choice)).toEqual({
			model: '',
			effort: ''
		});
	});
});
