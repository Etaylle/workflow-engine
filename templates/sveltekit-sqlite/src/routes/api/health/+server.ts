import { json } from '@sveltejs/kit';
import { getDb } from '#lib/server/db.js';

export function GET() {
	const row = getDb().prepare('select count(*) as notes from notes').get();
	return json({ status: 'ok', notes: Number(row?.notes ?? 0) });
}
