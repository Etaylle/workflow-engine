import { describe, expect, it } from 'vitest';
import { parseRoute, stepHash } from './route.js';

describe('parseRoute', () => {
	it('maps the empty hash to the list', () => {
		expect(parseRoute('')).toEqual({ name: 'list' });
		expect(parseRoute('#')).toEqual({ name: 'list' });
		expect(parseRoute('#projects')).toEqual({ name: 'list' });
	});
	it('maps #new to the new-project form', () => {
		expect(parseRoute('#new')).toEqual({ name: 'new' });
	});
	it('maps #<id>/steps to the team and steps page', () => {
		expect(parseRoute('#abc-123/steps')).toEqual({ name: 'steps', id: 'abc-123' });
	});
	it('maps any other hash to a project id', () => {
		expect(parseRoute('#abc-123')).toEqual({ name: 'project', id: 'abc-123', step: null });
	});
	it('maps #<id>/<step> to one step of a project', () => {
		expect(parseRoute('#abc-123/questions')).toEqual({
			name: 'project',
			id: 'abc-123',
			step: 'questions'
		});
		expect(parseRoute('#abc-123/nonsense')).toEqual({ name: 'project', id: 'abc-123', step: null });
	});
	it('writes the address of a step', () => {
		expect(stepHash('abc-123', 'run')).toBe('#abc-123/run');
	});
});
