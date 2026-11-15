import { describe, expect, it } from 'vitest';
import { normalisePrerequisites } from '#lib/api/execution.js';
import { actionFor, claudeAuthOptions, readinessOf, startBlock } from './builders.js';

const prerequisites = normalisePrerequisites({
	builders: [
		{ id: 'claude_code', ready: false, missing: ['anthropic_key'], details: null },
		{ id: 'gemini_cli', ready: true, missing: [] },
		{ id: 'other' as 'claude_code', ready: true }
	],
	claudeAuth: 'anthropic_key'
});

describe('builder prerequisites', () => {
	it('keeps known builders and tolerates absent lists', () => {
		expect(prerequisites.builders.map((b) => b.id)).toEqual(['claude_code', 'gemini_cli']);
		expect(readinessOf(prerequisites, 'gemini_cli')?.missing).toEqual([]);
		expect(normalisePrerequisites({}).builders).toEqual([]);
	});

	it('blocks the start only for a builder that is known and not ready', () => {
		expect(startBlock(prerequisites, 'claude_code')).toEqual({
			builder: 'claude_code',
			missing: ['anthropic_key']
		});
		expect(startBlock(prerequisites, 'gemini_cli')).toBeNull();
		expect(startBlock(null, 'claude_code')).toBeNull();
	});

	it('names an action for each missing thing', () => {
		expect(actionFor('anthropic_key')).toEqual({ kind: 'open_key_panel' });
		expect(actionFor('machine_sign_in')).toEqual({ kind: 'use_machine_sign_in' });
		expect(actionFor('surprise')).toEqual({ kind: 'none' });
	});

	it('marks which Claude Code authorisation is ready and chosen', () => {
		expect(claudeAuthOptions(prerequisites)).toEqual([
			{ mode: 'anthropic_key', ready: false, selected: true },
			{ mode: 'machine_sign_in', ready: true, selected: false }
		]);
	});
});
