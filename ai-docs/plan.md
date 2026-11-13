# The plan: goals, epics, stories and tasks of the workflow engine

The plan of the work, in the hierarchy of `workflow.md` 2: **goal** (`G-nn`) → **epic** (`E-nn`) → **user story** (`US-ee.ss`, the unit that is built, merged and deployed) → **task** (`T-ee.ss.n`, the work of one agent on one story). Where the work stands now: `../todo.md`. How to start or resume: `../kickoff-prompt.md`.

| Folder | One file per | Template | Written by |
|---|---|---|---|
| `ai-docs/goals/` | goal | `workflow.md` 17.1 | Product Owner (with the Lead) |
| `ai-docs/epics/` | epic | `workflow.md` 17.2 | Lead (scope by the Product Owner) |
| `ai-docs/stories/` | story card | `ai-docs/stories/_template.md` (`workflow.md` 4.1) | Lead, with `/start-story` |
| `ai-docs/tasks/` | task spec | `workflow.md` 17.4 | Lead; `completion_notes` by the agent |

Detail is refined just in time (WFL-005): the goals and all epics are written; the stories of the active epic are precise cards; stories of later epics are one line in their epic file and get a card when their epic becomes active.

## The plan at a glance

| Goal | Epic | Content | Status |
|---|---|---|---|
| [G-01](goals/G-01.md) Technical setup | [E-00](epics/E-00.md) | Repository, toolchain, skeleton page, local pipeline, local dev, test and prod, spike US-00.S1 | Planned |
| [G-02](goals/G-02.md) MVP business requirements | [E-01](epics/E-01.md) | `projects`: all projects, wish, files, session key, GitHub sign-in (screens 0, 1) | Proposed |
| | [E-02](epics/E-02.md) | `questions`: research, questions, technology choice (screen 2) | Proposed |
| | [E-03](epics/E-03.md) | `documents`: requirements and project document (screen 3) | Proposed |
| | [E-04](epics/E-04.md) | `workflows`: BPMN-like workflow, edit a task, contract (screens 4, 5, 8) | Proposed |
| | [E-05](epics/E-05.md) | `settings`: models per tier, editable AI team and development workflow (screens 6, 7) | Proposed |
| | [E-06](epics/E-06.md) | `execution`: sandbox, the run of `build-one-task`, stop, resume, push, ZIP, report (screens 9 to 13) | Proposed |
| | [E-07](epics/E-07.md) | Case epic "From wish to result": the cross-domain journey end to end | Proposed |
| [G-03](goals/G-03.md) Audit and testing | [E-08](epics/E-08.md) | Integration, audit and verification | Proposed |
| [G-04](goals/G-04.md) CI/CD and infrastructure | [E-09](epics/E-09.md) | Completion of the delivery path on the local machine (test and prod already exist, ADR-041); a remote host only if wanted | Proposed |
| | [E-10](epics/E-10.md) | Release of the MVP through dev, test, prod | Proposed |

States: goal and epic `Proposed → Planned → Active → Gate → Closed` (`workflow.md` 11); story `Draft → Ready → In progress → Verification → Done`; task `open → in progress → done | blocked` (17.4).

## Where each piece is built and deployed (as the documents define it)

| Unit | Built and checked | Deployed to | Rule |
|---|---|---|---|
| Task (`T-…`) | In the story branch by its agent; `ci/run` steps of its area | Not deployed alone: a task is part of a story | WFL-136, AGT-031 |
| Story (`US-…`) | `ci/run all-local` (local containers: build, check, images, `env-up layers`, migrate, seed, tests, smoke, report) | **local** containers, then after the merge **dev** on the local machine (`deploy dev`, `verify dev`, pointer `env/dev`) | WFL-130, INF-BRN-013, CIC 4.1 |
| Epic (`E-…`) | Epic gate (`workflow.md` 11) on the integrated state | **test** (`promote dev test`, `verify test`), then **prod** after the Product Owner's approval (`promote test prod`, `verify prod`) | WFL-131, WFL-133, INF-ENV-005, CIC-140, CIC-142 |

All environments run on the local machine with Docker until the presentation (ADR-041): `dev`, `test` and `prod` are separate compose projects with their own volume, env file and ports, created in Epic 0 (US-00.10, US-00.14). The deployment commands run on the local machine. A remote host is not planned; if one is wanted later (for example `amberneurons-engine`), its first deployment is the first story of G-04.
