# todo: where the work stands

Updated after each slice and each decision. Newest first. The plan: `ai-docs/plan.md`. How we work: `CLAUDE.md` 2.

Last update: 2026-11-14 (Claude): M0 walking skeleton complete (frontend, AI check, ci/run, end-to-end test); D-32 to D-34.

## Current position

| Item | State |
|---|---|
| Milestone | M0 Walking skeleton done 2026-11-14; next M1 thin workflow path |
| M0 done | `backend/` (Spring Boot 4.1.2, Java 21 by Gradle toolchain, wrapper 8.14.7), the four planning migrations (18 tables), `GET /api/status` from `api/openapi.yaml`, health; `./gradlew test` 2 of 2 green (unit, architecture), `./gradlew integrationTest` 3 of 3 green on PostgreSQL 17 |
| M0 also done | `frontend/` (SvelteKit 3, Svelte 5, English and German, status call, AI check), `POST /api/ai/check` (key in memory only, Anthropic Java SDK, `claude-haiku-5-5`; stub with `ENGINE_AI_STUB=true`), `ci/run`, one Playwright journey; `ci/run all-local` green (backend 5 unit + 3 integration, frontend 5 Vitest, 1 end-to-end). Not tried: the AI check with a real valid key (only the 401 path against the live API) |
| Product code | skeleton only (M0): `backend/`, `frontend/`, `api/`, `ci/` are created in M0; the tables and records to start from are in `ai-docs/skeleton/` |
| Git | Local only (D-24). The remote `origin` still points to the earlier project repository; do not push to it |

## This machine

| Tool | Found 2026-11-14 | Needed |
|---|---|---|
| Java | Temurin 17 | 21: Gradle downloads it into the project (toolchain) |
| Gradle | not installed | the wrapper in `backend/` (`./gradlew`); Gradle opens a local socket, so Claude Code runs it outside its sandbox or with `sandbox.network.allowLocalBinding` |
| Node, npm | 24.13.0, 11.6.2 | ok |
| Docker Desktop | installed, not running | not before the second half of M3 |
| PostgreSQL | 17 (Homebrew) | used for tests while Docker is off |

## Decisions needed (owner)

| ID | Decision | Blocks | Recommendation |
|---|---|---|---|
| GitHub | The owner's own repository for this project | the first push | Create it when M0 is green |
| OI-2, 3, 8, 9 | Open issues of `requirements.md` Appendix A (presentation access, audit report in the ZIP, MCP door, model per agent) | M3 to M6 | Decide when the milestone starts |

## Decisions taken

| ID | Decision | Date |
|---|---|---|
| D-34 | `ci/run` starts the engine jar with the Java 21 of the Gradle toolchain (`gradle printJavaHome`), because the system Java is 17; engine logs and pid files in `.run/` (ignored); tests use `engine_test`, the running app `engine`; end-to-end engine runs on port 8081 | 2026-11-14 |
| D-33 | The frontend is built by Gradle (`frontendBuild`) and copied into the jar as `static/`: one process, one port. Frontend text lives in `frontend/src/lib/i18n/messages/{en,de}.json` with a small own translate function (no i18n library); a test keeps both files in step | 2026-11-14 |
| D-32 | SvelteKit 3 differences: configuration is in `vite.config.ts` (no `svelte.config.js`), imports use `#lib/...js` (declared in `package.json` `imports`), not `$lib`. TypeScript 6 (npm resolved 6.x) | 2026-11-14 |
| D-31 | The owner works alone: no team, no second reviewer (CON-02 and CON-06 of `requirements.md` do not apply) | 2026-11-14 |
| D-30 | By 20 November 2026 the workflow must work: a wish becomes a workflow in the browser. Order: M0, M1 thin workflow path, M2 planning complete, then the build spike M3 and the run. No Docker and no key in a file are needed before M3; the build spike starts in a workspace folder with Claude Code's own sandbox, then one container (answers D-22) | 2026-11-14 |
| D-28 | The schemas `settings` and `execution` are not created yet: they were designed before any run existed and stay drafts in `ai-docs/skeleton/` until M3. The backend creates only `projects`, `questions`, `documents` and `workflows` | 2026-11-14 |
| D-27 | Planning calls use the official Anthropic Java SDK, not Spring AI (replaces TR-06): a client per call from the session key, structured output by class, less code between the engine and the API. Model tiers: best `claude-opus-5-5`, standard `claude-sonnet-5-5` | 2026-11-14 |
| D-26 | Spring Boot 4.1 instead of 3 (replaces that part of TR-01): the 3.x line is out of open-source support since 30 June 2026; 4.1 is supported until July 2027. Java stays 21 | 2026-11-14 |
| D-25 | One environment on the local machine (Docker Compose) until M7; no separate dev, test and prod, no promotion, no pointer branches (replaces D-05 and the environment part of ADR-041) | 2026-11-14 |
| D-24 | Work only on the owner's local machine. No push to the earlier remote `ekarimova/workflow-engine`; commits go later to the owner's own GitHub repository (replaces D-09, D-12, D-14, D-15, D-16) | 2026-11-14 |
| D-23 | Screens: a link to try the built software after each finished task; at most 8 questions at a time, each with the AI's proposal preselected; "Team and steps" is reachable from "Ready to run" and is not a required step | 2026-11-14 |
| D-21 | Claude Code is the only harness in the MVP (answers OI-4; TR-07); one template setup Java × Svelte × PostgreSQL is the MVP, the others are shown as "coming soon" (TR-11, FR-STK-01, FR-STK-02) | 2026-11-14 |
| D-20 | The technology rule documents (API, backend, database, frontend, i18n, tests, CI/CD, infrastructure, UX/UI, audit) are not used and were removed; quality rules are checks in the build (formatter, linter, type check, architecture test, tests) | 2026-11-14 |
| D-19 | The development steps of a task are data. Two profiles are seeded, the 16 steps and a short one (plan, build with tests, continuous integration, independent check); the build spike M3 measures both; the faster one that passes is the default (ADR-039 reopen trigger checked early) | 2026-11-14 |
| D-18 | Way of working: one developer with Claude Code, small vertical slices, `ci/run all-local` as the gate. The role agents, story cards, task specs, the 16 stages and the verdict rounds for building the engine were removed. The 16 steps and the team stay as product content in `ai-docs/reference/` | 2026-11-14 |
| D-17 | Order of the work by risk: milestones M0 to M7 of `ai-docs/plan.md`; goals G-01 to G-04 and epic E-00 are replaced by them | 2026-11-14 |
| D-13 | Target for 20 November: the path from the wish to the reviewed workflow works end to end with Claude; execution prepared, not built | 2026-11-14 |
| D-11 | AI from the very beginning: an AI connection check with the user's own key in the skeleton | 2026-11-14 |
| D-10 | No example workflow in the product for now; one may be used as test data (see D-22) | 2026-11-14 |
| D-08 | Requirements and project documents are generated after the answers (screen 3) and updated from the contract at confirmation | 2026-11-13 |
| D-07 | Skeleton domain `status`, replaced by the first business slice | 2026-11-13 |
| D-06 | Runs do not ask the client, except when a defect is not fixable; when tokens run out the run waits and continues | 2026-11-13 |
| D-01 | Everything on the local machine with Docker; no remote host (ADR-041) | 2026-11-13 |

## Corrections still open in the documents

| Document | Place | Open |
|---|---|---|
| Both Word masters in `ai-docs/word/` | whole file | Do not contain `requirements.md` 0.4 and 0.5 and `engine-architecture.md` 0.4 |
| `engine-architecture.md` | figures 3 and 5.6, steps of 6.3; test strategy 8; references to the removed rule documents | Written for the earlier plan; update when M3 is designed |
| `requirements.md` | BR-02 and QR-PERF-01 | The same 4-hour target once as Must and once as Should |
| `requirements.md` | TR-01, TR-06, TR-07, TR-09, TR-10, TR-11, FR-STK-01, FR-STK-02, FR-TIME-06, CON-06 | Bring in line with D-18, D-21, D-26, D-27 and the results of M3 (sandbox, runner image, cost limit) |
| `audit/deferred-risks.md` | column "Accepted by" | Placeholder "Product Owner, date" in 4 rows |
| `skeleton/backend/settings/seeds/default_team_profile.sql` | member instructions | Name agent files that do not exist and configuration file names; clean when the seed is placed in M0 |
| `ai-docs/reference/` | `architecture.md`, `workflow.md`, `team.md` | Documents of the earlier setup, kept for the decision register and as the source of the seeded team and steps; they cite removed documents |

## Next (in order)

1. M1 thin workflow path, until 20 November 2026 (live check: the owner types a key into the page).
2. M2 planning complete, as far as the time reaches.

## Done

- 2026-11-14 Lean setup: new `CLAUDE.md`, `ai-docs/plan.md` and this file; technology rule documents, role agents, process skills, story cards, task specs, goals and the epics E-00, E-08 to E-10 removed (Git history keeps them); `workflow.md` and `team.md` moved to `ai-docs/reference/`; `requirements.md` 0.5 and `engine-architecture.md` 0.4 aligned with the decisions taken.
- 2026-11-13 Documents, mockups, BPMN model and code skeleton written.
