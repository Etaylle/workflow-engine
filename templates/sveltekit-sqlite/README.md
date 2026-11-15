# App name

One or two sentences: what this app is for and who uses it. (The builder replaces this text in the
first task and keeps the whole file current in every task.)

## What it does

- A short list of what a user can do today, one line per feature.

## Prerequisites

- Node.js 24 or newer. No database server: the data is one SQLite file.

## Start

```
./start.sh
```

Installs, builds and starts the app. Open http://localhost:18080 (set `PORT` for another port,
`APP_DB_FILE` for another database file; default `data/app.db`).

## Check

```
./check.sh
```

Installs, type-checks, tests and builds. It prints `CHECK PASSED` when all is green.

## Where things are

- `src/routes/`: pages (`+page.svelte`) and API routes (`api/.../+server.ts`).
- `src/lib/server/db.ts`: the SQLite database (`node:sqlite`); `migrations/NNN_name.sql`: plain SQL
  migrations applied at start, forward-only.
- `CONTRACT.md`: what was agreed with the client.
