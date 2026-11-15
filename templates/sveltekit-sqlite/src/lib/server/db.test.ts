import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { DatabaseSync } from 'node:sqlite';
import { expect, test } from 'vitest';
import { migrate } from './db';

test('migrations apply once and the table is usable', () => {
	const dir = mkdtempSync(join(tmpdir(), 'mig-'));
	writeFileSync(join(dir, '001_notes.sql'), 'create table notes (id integer primary key, text text)');
	const db = new DatabaseSync(':memory:');

	expect(migrate(db, dir)).toEqual(['001_notes.sql']);
	expect(migrate(db, dir)).toEqual([]);
	db.prepare('insert into notes (text) values (?)').run('hello');
	expect(db.prepare('select count(*) as n from notes').get()?.n).toBe(1);
});
