# Generated project: builder guide

One TypeScript project: SvelteKit 3 (Svelte 5) with pages and API routes, built with `adapter-node`,
and SQLite through Node's built-in `node:sqlite` (file `data/app.db`, `APP_DB_FILE`). Tests with Vitest.

## Rules of this kind of software

- No database server, no second backend, no second build system: pages in `src/routes/`, API in
  `src/routes/api/**/+server.ts`, data access in `src/lib/server/`. Import shared code as
  `#lib/server/db.js` (the `imports` map of `package.json`), not `$lib`.
- Schema changes are new plain SQL files `migrations/NNN_name.sql` (forward-only, never edit an
  applied one); they are applied when the server starts.
- The database is available, not obligatory: use it only where the wish or a requirement needs data
  to outlive the session; otherwise keep state in memory and add no server routes, tables or network
  calls the wish does not need.
- Add dependencies only when the task needs them. Never put a secret into a file.

## Commands

- `./check.sh` installs, type-checks (`svelte-check`), tests (Vitest) and builds. A step is green only
  when it prints `CHECK PASSED`.
- `./start.sh` is the one command that runs the app from a fresh unzip: installs, builds, starts on
  `PORT` (default 18080) and prints the URL. The engine uses it for the client's preview.

## Rules

- Every new behaviour gets a test (`src/**/*.test.ts`). Where the app uses SQLite, test the real one
  (in memory or a temp file); do not mock the database.
- Fix every warning and type error; never suppress one. Never weaken or delete a test to get green.
- Keep `./start.sh` working: when the way the app is built or started changes, change it with it.
  What the app prints to its console is shown to the client.
- Update `README.md` in every task: what the app is, what it does now, prerequisites, `./start.sh`,
  `./check.sh` and where things are. It must match the code.
- Work only inside this folder. Commit nothing: the orchestrator commits after a green step.
