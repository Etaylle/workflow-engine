import { DatabaseSync } from 'node:sqlite';
import { mkdirSync, readdirSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';

/** Applies every migrations/NNN_name.sql once, in name order (forward-only). */
export function migrate(db: DatabaseSync, dir: string): string[] {
	db.exec('create table if not exists schema_migrations (name text primary key, applied_at text not null)');
	const applied = new Set(
		db
			.prepare('select name from schema_migrations')
			.all()
			.map((row) => String(row.name))
	);
	const files = readdirSync(dir)
		.filter((file) => file.endsWith('.sql'))
		.sort();
	const done: string[] = [];
	for (const file of files) {
		if (applied.has(file)) continue;
		db.exec('begin');
		try {
			db.exec(readFileSync(join(dir, file), 'utf8'));
			db.prepare('insert into schema_migrations (name, applied_at) values (?, ?)').run(
				file,
				new Date().toISOString()
			);
			db.exec('commit');
		} catch (error) {
			db.exec('rollback');
			throw new Error(`Migration ${file} failed: ${String(error)}`, { cause: error });
		}
		done.push(file);
	}
	return done;
}

function open(): DatabaseSync {
	const file = resolve(process.env.APP_DB_FILE ?? 'data/app.db');
	mkdirSync(dirname(file), { recursive: true });
	const db = new DatabaseSync(file);
	migrate(db, resolve(process.env.APP_MIGRATIONS_DIR ?? 'migrations'));
	return db;
}

let instance: DatabaseSync | undefined;

/** The one database of the app; the file is created and migrated on first use. */
export function getDb(): DatabaseSync {
	instance ??= open();
	return instance;
}
