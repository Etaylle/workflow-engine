# Kickoff: how to start (or resume) work on the workflow engine

For every developer, human or AI, on day one or in the middle of the project. Three things to read first:

| File | Tells you |
|---|---|
| [`todo.md`](todo.md) | **Where the work stopped**: current goal, epic, story and task; decisions waiting for a human; what is next; what is done; what runs where |
| [`ai-docs/plan.md`](ai-docs/plan.md) | The whole plan: goals G-01 to G-04, epics E-00 to E-10, how stories and epics are deployed |
| [`CLAUDE.md`](CLAUDE.md) | The rules every session keeps |

Everything else is reached from there: `ai-docs/README.md` (index, product decisions, the 14 screens, the BPMN model), `ai-docs/goals/`, `ai-docs/epics/`, `ai-docs/stories/` (story cards with their checkpoint), `ai-docs/tasks/` (task specs).

## 1. Set up the machine (once)

Follow `ai-docs/README.md` section 10: check the setup, the test key with a spend limit in `.env`, the GitHub CLI. Then:

```bash
node .claude/hooks/selftest.mjs                 # guard: all cases pass
node .claude/skills/docs-check/check-refs.mjs   # documents: 0 broken references
```

## 2. Find where the work stands

1. Open `todo.md`, section **Current position**: goal → epic → story → task, and its state.
2. Open the story card it names (`ai-docs/stories/US-ee.ss.md`) and read its **Checkpoint**: last verified commit and run, next step, open questions (AGT-067, AGT-068). Resume from the checkpoint, not from memory.
3. Check **Decisions needed** in `todo.md`: a story that depends on an open decision is not Ready (WFL-013).

## 3. Start Claude Code

Start `claude` in the project root, switch to **plan mode** (Shift+Tab), and paste the prompt that fits.

### 3a. First session of the project (nothing built yet)

Run the skill `/kickoff-epic0`, or paste:

> You are the Lead of this project in plan mode. Do not change any file yet. Read `CLAUDE.md`, `todo.md`, `ai-docs/plan.md`, `ai-docs/goals/G-01.md`, `ai-docs/epics/E-00.md`, and the skill `project-learnings`. Then read, only as far as needed: `.claude/skills/architecture/architecture.md` sections 2, 4.1, 5, 6, 7; `.claude/skills/workflow/workflow.md` 4, 5.1, 8, 9; `.claude/skills/infrastructure/infrastructure.md` 17; `ai-docs/engine-architecture.md` 2, 4, 5.1, 7; `ai-docs/requirements.md` 1.4, 7, 10.
>
> Check the Epic 0 plan in `ai-docs/epics/E-00.md`, its story cards and task specs against these documents. Report: (1) gaps or contradictions (stop and ask, CLAUDE.md 3); (2) the decisions still open in `todo.md`, each with your recommendation (the decisions of 13 November 2026 hold: everything on the local machine with Docker, ADR-041; own Git repository; Java 21, Node 24, npm, Gradle Kotlin DSL; `requirements.md` 0.4); (3) the order of the stories and which subagents work on each (WFL-050; subagents cannot start subagents, the main session starts them); (4) the spike US-00.S1 (agent harness, OI-4). The skeleton in `ai-docs/skeleton/` must be kept and built around (`ai-docs/software-elements.md`). After my approval, update the cards and `todo.md`; I start the first story with `/start-story US-00.01`.

### 3b. Resume in the middle of the project

> You are the Lead in plan mode. Do not change any file yet. Read `CLAUDE.md`, `todo.md` (current position, decisions, next), the card of the current story in `ai-docs/stories/` with its checkpoint, its task specs in `ai-docs/tasks/`, and the file of its epic in `ai-docs/epics/`. Check with `git log --oneline -10` and `git status` that the repository matches the checkpoint. Tell me: where the work stopped, what is verified and what is not, the next step, and what blocks it. Then wait for my approval.

## 4. The loop for every story

| Step | Who | Command or skill |
|---|---|---|
| 1. Card, Definition of Ready, plan, task specs | Lead (subagent `lead`), human approves | `/start-story US-ee.ss` |
| 2. Build, task by task | Role subagents (`architect`, `ux-ui-designer`, `backend-developer`, `frontend-developer`, `tester`, `devops`) started by the main session | the task spec `ai-docs/tasks/T-….md` |
| 3. Local containers | Author | `ci/run all-local` (`ci/run env-up layers` to look at it) |
| 4. Team verification, evidence | All active roles, `reviewer` | `/team-verification US-ee.ss` |
| 5. Merge | Human | squash-merge the story branch after the Reviewer's approval (a pull request with `gh pr create` once the engine has a GitHub repository, D-09); agents never push to `main` |
| 6. Dev | Author, on the local machine after the merge | `ci/run deploy dev`, `ci/run verify dev` → pointer `env/dev` |
| 7. Record | Author of the step | update the story checkpoint and `todo.md` |

At the **end of an epic**: epic gate (`workflow.md` 11, `/audit-report AP2 E-nn`), then promotion of the same build: `ci/run promote dev test`, `ci/run verify test`, the Product Owner's approval, `ci/run promote test prod`, `ci/run verify prod`. Dev, test and prod are compose projects on the local machine (ADR-041), created in Epic 0 (US-00.10, US-00.14); before they exist the promotion reports them "not available" (`ai-docs/plan.md`).

## 5. Keep `todo.md` true

After every task, story, decision or deployment: move the item, add the date, update **Current position** and **Deployments**. If development stops in the middle, `todo.md` and the story checkpoint are what the next person reads.
