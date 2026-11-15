import { getDb } from '#lib/server/db.js';

export function load() {
	const notes = getDb().prepare('select id, text from notes order by id').all();
	return { notes: notes.map((note) => ({ id: Number(note.id), text: String(note.text) })) };
}
