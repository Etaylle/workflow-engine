# todo: where the work stands

Kept up to date by everyone who works on the engine (human or agent): after each task, story, decision or deployment (`kickoff-prompt.md` 5). Newest entries first. The plan: `ai-docs/plan.md`.

Last update: 2026-11-13, main session (Claude): decisions D-01 to D-08 of the Product Owner recorded.

## Current position

| Level | Item | State |
|---|---|---|
| Goal | [G-01 Technical setup](ai-docs/goals/G-01.md) | Planned (due 2026-11-20) |
| Epic | [E-00 Technical project setup](ai-docs/epics/E-00.md) | Planned; kickoff plan not yet approved |
| Story | [US-00.01 Repository and layout](ai-docs/stories/US-00.01.md) | Draft (Definition of Ready not checked) |
| Task | [T-00.01.2](ai-docs/tasks/T-00.01.2.md) (human: first commit, pointer branches), then [T-00.01.1](ai-docs/tasks/T-00.01.1.md) | open |

The Git repository exists (`git init`, branch `main`, no commit yet). No product code exists: `backend/`, `frontend/`, `api/`, `ci/`, `infrastructure/` are not created. The code skeleton waits in `ai-docs/skeleton/`.

**Next action (human):** run the guard update (D-04), make the first commit (D-02), then `/kickoff-epic0` in plan mode (`kickoff-prompt.md` 3a).

```bash
node .claude/skills/project-learnings/apply-guard-paths.mjs && node .claude/hooks/selftest.mjs
git add -A && git commit -m "US-00.01 Documents, agents, skills and plan of the workflow engine"
```

## Decisions needed (human: Product Owner and approving Architect)

| ID | Decision | Blocks | Recommendation | State |
|---|---|---|---|---|
| D-04 | Guard path rules: new document paths, plan files for the lead, `todo.md` for every role | Every role subagent that writes a document or `todo.md` | Run the script above; then delete it | approved 2026-11-13, **to be applied by the human** (AGT-081) |
| D-09 | GitHub repository for the engine (pull-request runs, US-00.09; business goal G4 "another team runs the engine from GitHub") | US-00.09, E-10 | Create it before the presentation, private until then | open |
| OI-2, 3, 5, 8, 9 | Open issues of `requirements.md` Appendix A (presentation access, audit report in the ZIP, cost limits, MCP door, model per agent) | E-04, E-06 | Decide when the epic is planned | open |
| OI-4 | Default harness: Claude Code, opencode or both | E-06 | Answered by the spike US-00.S1 | open |

## Decisions taken

| ID | Decision | Date | Recorded in |
|---|---|---|---|
| D-01 | Everything on the local machine with Docker until the presentation; maybe never remote. Dev, test, prod are compose projects on it. A later host name may be amberneurons-engine (.com, .at or .ai) | 2026-11-13 | ADR-041 (`architecture.md` 5), `CLAUDE.md` 9, `ai-docs/plan.md` |
| D-02 | `workflow-engine` is its own Git repository and stays inside `~/DEV`; `~/DEV` ignores it (`~/DEV/.gitignore`, its staged entries of this folder removed from the index) | 2026-11-13 | `git init -b main` done; first commit by the human |
| D-03 | Use what is installed: Java 21.0.12, Gradle wrapper (Kotlin DSL), Node 24.14.1, npm 11 (as in `ci-cd.md` 4); install only what a step needs | 2026-11-13 | `ai-docs/stories/US-00.02.md`, `CLAUDE.md` 9 |
| D-05 | Every merged story to local and dev; every finished epic promoted to test and prod (PO approval for prod), all local | 2026-11-13 | ADR-041; story US-00.14 added to E-00 |
| D-06 | Runs do not ask the client, except in the very rare case of a defect that is not fixable; when tokens run out (rate limit or credit) the run waits and continues instead of failing | 2026-11-13 | `requirements.md` 0.4: FR-EXE-04, FR-EXE-07, FR-EXE-08, new FR-EXE-11; E-06 (US-06.10) |
| D-07 | Skeleton domain: `status` (`workflow.md` 9.2); replaced by the first business story (WFL-074) | 2026-11-13 | US-00.03 |
| D-08 | Requirements and project documents are generated after the answers (screen 3) and updated from the contract at confirmation | 2026-11-13 | `requirements.md` 0.4: FR-DOC-01, FR-DOC-02; E-03 (US-03.04) |

The Word master of the requirements (`ai-docs/word/Workflow-Engine-Requirements-Specification.docx`) does not yet contain the 0.4 changes (human).

## Next (in order)

1. Human: D-04 script, first commit.
2. `/kickoff-epic0` in plan mode; approve the E-00 plan.
3. US-00.01 → US-00.02 → US-00.S1 (spike) → US-00.03 → US-00.04 → US-00.05 → US-00.06 → US-00.08 → US-00.10 (local dev) → US-00.11 → US-00.14 (local test, prod, promotion) → US-00.13; US-00.07, US-00.09 (needs D-09), US-00.12 after 20 November if time is short (`ai-docs/epics/E-00.md`).
4. E-00 gate: `/audit-report AP2 E-00`, promote the skeleton build dev → test → prod; G-01 sign-off; then E-01 (`/start-story US-01.01`).

## In progress

- nothing

## Done

- 2026-11-13 PO decisions D-01 to D-08 recorded (ADR-041, `requirements.md` 0.4, `CLAUDE.md` 9); own Git repository initialised; E-00 adapted (US-00.10 local dev, new US-00.14 local test and prod, US-00.09 only with GitHub); guard update script prepared and tested on a copy (48 cases pass).
- 2026-11-13 Plan written: `ai-docs/plan.md`, goals G-01 to G-04, epics E-00 to E-10, the story cards and task specs of E-00; `kickoff-prompt.md` and this file at the root.
- 2026-11-13 Documents restructured: one `ai-docs/README.md`; rule documents as skills in `.claude/skills/<area>/`; skeleton in `ai-docs/skeleton/`; new skills `screen-build`, `bpmn-model`, `find-rule`, `docs-check`, `agent-rules`, `project-learnings`, `kickoff-epic0`.

## Deployments

All environments are on the local machine (ADR-041).

| Environment | Build id | Story or epic | Date | Result |
|---|---|---|---|---|
| local | — | — | — | not available (no code yet) |
| dev | — | — | — | not available until US-00.10 |
| test | — | — | — | not available until US-00.14 |
| prod | — | — | — | not available until US-00.14 |
