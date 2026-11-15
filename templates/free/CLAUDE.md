# Generated project: builder guide

This workspace starts almost empty on purpose. You choose the technology that fits the wish in
`CONTRACT.md`; the engine prescribes only the contract below, not the insides.

## Choose the simplest technology that fits

Decide in the plan step, write the choice and one sentence of reason into `README.md` (section
Technology), and stay with it in later steps.

- Small game, animation, toy or tool that needs no stored data and no accounts: **one static page**
  (HTML, CSS, TypeScript or JavaScript, canvas where useful). No server, no database, no framework.
- Web application with several screens: **React + Vite + TypeScript**. Add a server (Node) only when
  the wish needs one, and a database (a SQLite file) only when data must outlive the browser session.
- Command-line or console program: **Node.js (TypeScript) or Python**, standard library first.
- Something else only when the wish or a requirement names it.

Prefer widely used, boring technology and few dependencies. Do not add a backend, a database, a
login, an API or a build step that the wish does not need.

When the wish names a kind of software with a well-known design (a classic game, a calculator, a
to-do list), build that known design completely: every part a user expects, from start to end.

## What may be installed

- First look at what this machine has: `command -v node npm python3 java go` and their versions.
  Choose among what is there.
- Install freely **inside this folder**: `npm install`, a Python virtual environment in `.venv`,
  project-local tools. `./check.sh` and `./start.sh` must do these installs themselves.
- Never install system-wide: no `brew`, `apt`, `sudo`, `npm install -g`, `pip install` outside a
  virtual environment, no `curl ... | sh`.
- Only when the wish or a requirement cannot be met with what is installed: write `NEEDS.md` with
  the tool, why it is needed, and the exact command the owner would run to install it; then stop
  and say so in your last line. The engine asks the owner. Do not try another way around it.

## The contract of every project (the engine relies on it)

- `./start.sh`: one command from a fresh unzip to the running software. It installs what is
  missing inside the folder, builds, and starts. A web page or app listens on the port in the
  environment variable `APP_PORT` (default 18080) and prints its address; a console program
  prints its output to the terminal and passes its exit code on.
- `./check.sh`: installs, runs the static checks the technology offers (types, linter) and all
  tests; prints the single line `CHECK PASSED` and exits 0 only when everything is green.
- `app.json`: `{"kind": "web"}` for anything opened in a browser, `{"kind": "console"}` for a
  program that runs in a terminal. Keep it true.
- `README.md`: what the software is, the technology and why, prerequisites, `./start.sh`,
  `./check.sh`, how to use it. Update it in every step that changes any of these.
- Both scripts must work from a fresh copy without the files that `.gitignore` leaves out.

## Rules

- The engine uses the software after your step: it runs `./start.sh`, and for a web page it opens
  the page in a browser, presses the main controls and fails the step on any script error, failed
  request or empty page. Use the software yourself the same way before you finish.
- The logic gets tests that would fail if the behaviour broke. Keep them proportionate: a small
  game needs tests of its rules, not of every pixel.
- Fix every warning of the tools you chose; never switch a check off to get green.
- Never put a secret into a file. Work only inside this folder. Commit nothing: the orchestrator
  commits after a green step.
