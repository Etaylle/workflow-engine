import { getDb } from '#lib/server/db.js';

// Open and migrate the database when the server starts, not on the first request.
getDb();
