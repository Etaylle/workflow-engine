# Generated project: builder guide

A Java 21 + Spring Boot backend (`backend/`), a Svelte 5 + Vite + TypeScript frontend
(`frontend/`) and PostgreSQL with Flyway migrations (`backend/src/main/resources/db/migration/`).

## Commands

- `./check.sh` builds and tests everything. A step is green only when it prints `CHECK PASSED`.
- Frontend only: `cd frontend && npm test`. Backend only: `cd backend && ./gradlew test`.
- Start: `cd backend && ./gradlew bootRun` (port 18080, serves the built frontend and `/api`).

## Rules

- One slice at a time: migration, API endpoint, backend code, frontend, tests together.
- Every new behaviour gets a test; tests start the real application on the real PostgreSQL
  (`APP_DB_URL`, `APP_DB_USER`, `APP_DB_PASSWORD`). Do not mock the database.
- The compiler runs with `-Werror` and `-Xlint:all`: fix every warning, never suppress one.
- Migrations are forward-only: add a new `V<n>__name.sql`, never edit an applied one.
- Add dependencies only when the task needs them. Never put a secret into a file.
- Never weaken or delete a test to get green.
- Work only inside this folder. Commit nothing: the orchestrator commits after a green step.
