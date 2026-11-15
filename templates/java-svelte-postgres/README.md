# App name

One or two sentences: what this app is for and who uses it. (The builder replaces this text in the
first task and keeps the whole file current in every task.)

## What it does

- A short list of what a user can do today, one line per feature.

## Prerequisites

- Java 21 or newer, Node.js 20 or newer, PostgreSQL 15 or newer running on `localhost:5432`.

## Start

```
./start.sh
```

It checks the prerequisites, creates the database `app` when it is missing, builds the app and starts
it. Open http://localhost:18080 (set `APP_PORT` for another port; `APP_DB_URL`, `APP_DB_USER` and
`APP_DB_PASSWORD` for another database).

## Check

```
./check.sh
```

Builds and tests everything. It prints `CHECK PASSED` when all is green.

## Where things are

- `backend/`: Java 21, Spring Boot, Flyway migrations in `backend/src/main/resources/db/migration/`.
- `frontend/`: Svelte 5, Vite, TypeScript.
- `docs/`: the plan and the check notes of each task.
- `CONTRACT.md`: what was agreed with the client.
