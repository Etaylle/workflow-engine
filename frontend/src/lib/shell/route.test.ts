import { describe, expect, it } from 'vitest';
import { parseRoute } from './route.js';

describe('parseRoute', () => {
	it('maps the empty hash to the list', () => {
		expect(parseRoute('')).toEqual({ name: 'list' });
		expect(parseRoute('#')).toEqual({ name: 'list' });
		expect(parseRoute('#projects')).toEqual({ name: 'list' });
	});
	it('maps #new to the new-project form', () => {
		expect(parseRoute('#new')).toEqual({ name: 'new' });
	});
	it('maps any other hash to a project id', () => {
		expect(parseRoute('#abc-123')).toEqual({ name: 'project', id: 'abc-123' });
	});
});
