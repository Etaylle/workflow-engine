import { describe, expect, it } from 'vitest';
import { normaliseBuilders } from '#lib/api/execution.js';
import { actionFor, builderOf, missingCodes, otherAuthMode, startBlock } from './builders.js';

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
		{ id: 'other' as 'claude_code', ready: true }
	]
});

describe('builder report', () => {
	it('keeps known builders and tolerates absent fields', () => {
		expect(report.builders.map((b) => b.id)).toEqual(['claude_code', 'gemini_cli']);
		expect(builderOf(report, 'claude_code')?.missing[0]?.instructionKey).toBe(
			'builders.claude.signInInstruction'
		);
		expect(normaliseBuilders({}).builders).toEqual([]);
	});

	it('blocks the start only for a known builder that is not ready', () => {
		expect(startBlock(report, 'claude_code')?.id).toBe('claude_code');
		expect(missingCodes(startBlock(report, 'claude_code')!)).toEqual(['not_signed_in']);
		expect(startBlock(report, 'gemini_cli')).toBeNull();
		expect(startBlock(null, 'claude_code')).toBeNull();
	});

	it('names an action for each missing thing', () => {
		expect(actionFor('no_anthropic_key')).toBe('open_key_panel');
		expect(actionFor('no_gemini_key')).toBe('open_key_panel');
		expect(actionFor('not_signed_in')).toBe('none');
		expect(actionFor('surprise')).toBe('none');
	});

	it('offers the other Claude Code sign-in mode', () => {
		const claude = builderOf(report, 'claude_code')!;
		expect(otherAuthMode(claude)).toBe('anthropic_key');
		expect(otherAuthMode({ ...claude, authMode: 'anthropic_key' })).toBe('machine_sign_in');
	});
});
