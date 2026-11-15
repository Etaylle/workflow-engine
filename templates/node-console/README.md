# App name

One or two sentences: what this program is for. (The builder replaces this text in the first task
and keeps the whole file current in every task.)

## What it does

- A short list of what the program does today, one line per behaviour.

## Prerequisites

- Node.js 24 or newer.

## Start

```
./start.sh
```

Prints its output in the terminal and ends by itself.

## Check

```
./check.sh
```

Installs, type-checks and tests. It prints `CHECK PASSED` when all is green.

## Where things are

- `src/main.ts`: the program. `src/*.test.ts`: tests (Node's test runner).
- `CONTRACT.md`: what was agreed with the client.
