# todo: where the work stands

Updated after each slice and each decision. Newest first. The plan: `ai-docs/plan.md`. How we work: `CLAUDE.md` 2.

Last update: 2026-11-15 (Claude): M6 slices 1 to 3 done (All projects with rename/delete, wish form and edit, app shell, edit/add/delete single questions); D-46 to D-50. M2 planning complete 2026-11-14 (real Claude calls not tried).

## How to test (owner)

**Start** (from the project folder): `ci/run env-up` builds the frontend and the jar, starts PostgreSQL if needed and the engine at **http://localhost:8080**. Stop it with `ci/run env-down`. All checks: `ci/run all-local`.

**With the stub, no key, no cost**: `ENGINE_AI_STUB=true ci/run env-up`. Every Claude answer is then the canteen example (any text you type gives the same brief, 3 questions, 5 requirements and a 6-step workflow); type any text as the key (for example `sk-test`).

**With a real key**: `ci/run env-up` (no `ENGINE_AI_STUB`). Type your Anthropic key into the "Anthropic key" field of the new-project form. It stays in engine memory only; after a restart enter it again. Calls use `claude-sonnet-5-5` and can take up to two minutes each.

**Click path** (the page is `/`; the open project is in the address after `#`, the list is `#projects`, so a reload keeps your place):

0. The start page is "All projects" (open, rename, delete with confirmation). "New project" opens "Describe your idea" (address `#new`).
1. Type the optional name, the wish and the key ("Test" checks the key), "Send to the AI".
2. "Research and ask questions": Claude writes the brief and at most 8 questions; the AI's proposal is preselected on each. Edit, delete or add single questions ("Add a question", "Ask again" replaces all), change answers that do not fit, "Save answers".
3. "Write the requirements with Claude": requirements grouped by epic.
4. "Write the workflow with Claude": swimlane diagram. "Write it again" replaces it.
5. "Review and confirm" shows the counts; "Confirm as contract" freezes wish, brief, answers, requirements and workflow as contract version 1. Changing the workflow and confirming again makes version 2. "Edit the wish" under the project name moves the project back to step 1.

Reload the page at any step: nothing is lost. A wrong or missing key, a rate limit or an invalid answer shows a message and leaves the project where it was.

## Current position

| Item | State |
|---|---|
| Milestone | M0, M1 and M2 done 2026-11-14; next M3 (build spike, not started) |
| M2 done | Brief (`POST/GET /api/projects/{id}/brief`), questions (`POST/GET .../questions`, `PUT .../answers`), requirements (`POST/GET .../requirements`), workflow now from answers and requirements; contract snapshot holds wish, brief, answers, requirements, workflow. Frontend: one project page with the steps, status-driven so a reload keeps the place. Diagram redrawn with swimlane bands (screenshot checked). `ci/run all-local` green (counts in the report of this session). **Not tried: any real Claude call of M2** (4 calls: brief, questions, requirements, workflow) |
| M0 done | `backend/` (Spring Boot 4.1.2, Java 21 by Gradle toolchain, wrapper 8.14.7), the four planning migrations (18 tables), `GET /api/status` from `api/openapi.yaml`, health; `./gradlew test` 2 of 2 green (unit, architecture), `./gradlew integrationTest` 3 of 3 green on PostgreSQL 17 |
| M0 also done | `frontend/` (SvelteKit 3, Svelte 5, English and German, status call, AI check), `POST /api/ai/check` (key in memory only, Anthropic Java SDK, `claude-haiku-5-5`; stub with `ENGINE_AI_STUB=true`), `ci/run`, one Playwright journey; `ci/run all-local` green (backend 5 unit + 3 integration, frontend 5 Vitest, 1 end-to-end). Not tried: the AI check with a real valid key (only the 401 path against the live API) |
| M1 done | Create project from wish (key kept in session), `POST /api/projects/{id}/workflow` (Claude Sonnet 5.5 via Anthropic Java SDK, structured output, validate, one retry; stub canteen with `ENGINE_AI_STUB=true`), Svelte Flow diagram (read-only, swimlane layout from the engine), confirm as immutable contract version (trigger in V5), project list survives reload (`#<id>`). `ci/run all-local` green: backend unit 13, integration 13, frontend Vitest 11, Playwright 2. **Not tried: the real Claude call** (owner: start the engine without `ENGINE_AI_STUB`, type a key into the new-project form, click "Write the workflow with Claude") |
| Product code | M0 skeleton and M1 slice: `backend/`, `frontend/`, `api/`, `ci/` are created in M0; the tables and records to start from are in `ai-docs/skeleton/` |
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
| D-50 | US-02.03: `POST /api/projects/{id}/question-items`, `PUT/DELETE .../question-items/{questionId}` (add, rewrite, delete one question; at most 8; only status `questions_ready`, 409 otherwise; an edit drops that question's answer; delete renumbers). Regenerate = "Ask again" (existing call). Saving answers with no questions is 400 | 2026-11-15 |
| D-49 | US-01.08: `PATCH /api/projects/{id}` takes name and/or wish; a changed wish sets status draft (renaming or the same wish does not). Questions of an earlier wish stay visible only as the prompt to ask again (the old questions are replaced then); requirements and workflow remain until redone; a confirmed contract is untouched and the next confirmation is version N+1 | 2026-11-15 |
| D-48 | Left out of M6 slice 1 (stories need later milestones or were cut in D-45): links and files (US-01.03), GitHub sign-in (US-01.06), opencode (D-21; only Claude Code is shown), GitHub column of the list | 2026-11-15 |
| D-47 | Deleting a project removes everything the domains hold for it: each domain implements `engine.shared.ProjectCleanup`, `ProjectService.delete` runs them in one transaction. Contracts stay undeletable (V5 trigger) except inside that transaction: V6 lets the delete pass when `engine.deleting_project` is set for the transaction. Rename = `PATCH /api/projects/{id}`, create takes an optional `name` | 2026-11-15 |
| D-46 | Frontend shell: global `src/app.css` (tokens, buttons, cards, pills), a hash router (`#projects` list, `#new` wish form, `#<id>` project), header with step chips from the project status. The AI check section and the status block of the old page moved: key test is on the wish form, engine name and build in the page footer | 2026-11-15 |
| D-45 | Left out of the M2 stories to stay on the path: attachments and links in the brief (US-01.07 keeps only the brief table fields goal, actors, domain terms, risks), web research with sources (US-02.04: the questions call uses Claude's own knowledge), edit/add/delete of single questions (US-02.03), technology choice, languages and colours (US-02.05/06), project plan document, download as DOCX/PDF (US-03.02 to 03.04), cost limit, time rules, GitHub and Render on the review page (US-04.06 shows counts and the diagram, confirm only) | 2026-11-14 |
| D-44 | Status gates: brief only in `draft`; questions in `draft` or `questions_ready` (asking again replaces them); answers in `questions_ready` or `answered` (every question needs one answer, saved all at once); requirements in `answered` or `requirements_ready`; workflow in `requirements_ready`, `workflow_drafted` or `confirmed`. Anything else is 409. Accepting the proposal (value equal to it) is stored as `decided_by = ai`, any other value as `client` | 2026-11-14 |
| D-43 | Domains share data through ports in `engine.shared`: `ProjectPort` (wish, status, brief text, mark status), `AnswersPort` (implemented by `questions`), `RequirementsPort` (implemented by `documents`). `ClaudeCall` in `engine.shared` is the one place that builds the client from the session key, reads structured output, maps errors (401, 429, 502) and runs the one retry (`validated`); all four planning calls use it. `InvalidAiOutputException` moved there | 2026-11-14 |
| D-42 | Each planning call has an interface and a canteen stub (`ENGINE_AI_STUB=true`): `BriefWriter`, `QuestionWriter`, `RequirementWriter`, `WorkflowWriter`. The M1 direct path (workflow from the wish alone) is replaced: `WorkflowWriter.write` takes the planning material (brief, answers, requirements) and the endpoint answers 409 before the requirements exist | 2026-11-14 |
| D-41 | Questions: at most 8 (validated, retry on more), answer types single_choice (2 to 6 options), multiple_choice, text; the proposal is stored as JSON in `ai_default`; requirement keys are made by the engine (E-01, FR-001, QR-001, CON-001) | 2026-11-14 |
| D-40 | Contract snapshot is now wish + brief + answers + requirements + workflow view (still frozen by the trigger of V5; no new migration was needed, the tables of V1 to V4 carry everything) | 2026-11-14 |
| D-39 | `engine.shared` holds what domains share: `SessionKey` (moved from `ai`), `ProjectPort` (workflows reads the wish and sets the project status through it; `projects` implements it), `ApiProblem` (problem+json). Domains still never call each other | 2026-11-14 |
| D-38 | Workflow generation: one call, model `claude-sonnet-5-5`, no thinking or effort settings, 120 s timeout, SDK retries off; rules checked by `WorkflowValidator` (one start, an end, lanes known, reachable, no dead ends); on invalid output one retry with the reason; then 502. No server-side refusal fallback yet. Errors: 409 key missing, 401 key rejected, 429 rate limit, 502 failed/invalid. Positions come from a swimlane layout in the backend (x by steps from start, y by lane), stored in `nodes.pos_x/pos_y` | 2026-11-14 |
| D-37 | Contracts: V5 makes `cost_limit_amount` nullable (the limit is chosen in M3) and adds a trigger that rejects UPDATE and DELETE on `workflows.contracts`. Snapshot = wish + workflow view. One workflow per project; generating again replaces the draft (status valid, version+1); confirming needs status valid, so an unchanged confirmed workflow answers 409; a changed one confirms as version N+1 | 2026-11-14 |
| D-36 | Project name is the first line of the wish (60 characters); `agent_harness`, `provider`, `model` are set to claude_code, anthropic, claude-sonnet-5-5 until the settings domain exists. The key is optional on create and kept in memory only | 2026-11-14 |
| D-35 | Frontend: one page (`/`) with new project, project list, workflow panel; the open project is in the URL hash. Playwright runs with one worker because the engine holds one session key. Error messages are mapped from problem status/title to message keys (`#lib/api/problem.ts`) | 2026-11-14 |
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

1. Live check of M1 and M2 with a real key (owner, see "How to test").
2. M3 build spike.

## Done

- 2026-11-14 Lean setup: new `CLAUDE.md`, `ai-docs/plan.md` and this file; technology rule documents, role agents, process skills, story cards, task specs, goals and the epics E-00, E-08 to E-10 removed (Git history keeps them); `workflow.md` and `team.md` moved to `ai-docs/reference/`; `requirements.md` 0.5 and `engine-architecture.md` 0.4 aligned with the decisions taken.
- 2026-11-13 Documents, mockups, BPMN model and code skeleton written.
