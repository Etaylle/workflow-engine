# Generated project: builder guide

A console program in TypeScript on Node.js 24 (`src/main.ts`). Node runs the TypeScript directly
(type stripping): use `.ts` in import paths and no enums or namespaces.

## Rules of this kind of software

- This is a console program: output goes to stdout, input (if the wish needs any) comes from stdin
  or arguments. It must exit by itself unless the wish says it runs until stopped.
- No server, no database, no web page, no HTTP endpoint. Do not add them.
- Add dependencies only when the task needs them. Never put a secret into a file.

## Commands

- `./check.sh` installs, type-checks (`tsc`) and runs the tests (`node --test`). A step is green only
  when it prints `CHECK PASSED`.
- `./start.sh` runs the program from a fresh unzip. The engine shows its output to the client.

## Rules

- Every new behaviour gets a test (`src/*.test.ts`, `node:test`). Do not mock your own logic.
- Fix every type error; never suppress one. Never weaken or delete a test to get green.
- Keep `./start.sh` and `./check.sh` working; what the program prints is what the client sees.
- Update `README.md` in every task: what the program is, what it does now, prerequisites,
  `./start.sh`, `./check.sh`. It must match the code.
- Work only inside this folder. Commit nothing: the orchestrator commits after a green step.
