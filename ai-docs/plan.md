# The plan of the workflow engine

Where the work stands: [`../todo.md`](../todo.md). How we work: [`../CLAUDE.md`](../CLAUDE.md) 2. This plan replaces the plan of 13 November 2026 (goals G-01 to G-04, epic E-00 with its story cards and task specs, the gates and the three local environments); the reasons are at the end.

## Milestones

The owner's target for 20 November 2026 is that the workflow works: a wish becomes a workflow in the browser (D-30). So the planning path comes first, then the build, proven by a spike before it is built out. One session is about 3 hours of one developer with Claude Code. The session numbers are estimates; they are replaced by measured values in `todo.md`.

| # | Milestone | Content | Proof | Sessions |
|---|---|---|---|---|
| M0 | Walking skeleton | Repository layout, Gradle and npm builds, PostgreSQL with the planning schemas, backend with health and one API operation from `api/openapi.yaml`, SvelteKit page in English and German that calls it, AI check with the user's own key, `ci/run all-local` | The page runs from a clean clone with one command; tests green | 3 |
| M1 | Thin workflow path | Wish and session key → Claude writes the workflow in one call → shown as a diagram → confirm as contract (US-01.01, 01.05, 04.01, 04.02, 04.07), each in its thinnest version | The canteen wish becomes a confirmed workflow in the browser, with a real Claude key typed into the page | 4 |
| M2 | Planning to the contract, complete | Brief, questions (at most 8 at a time), answers, requirements, review page (US-01.07, 02.01, 02.02, 03.01, 04.06); the workflow is generated from the answers and requirements | The journey of screens 1 to 4 and 8 with a real key | 6 |
| M3 | Build spike | Without any screen: one business task of a confirmed canteen contract built by Claude Code headless from the Java × Svelte template, first in a workspace folder on the machine, then in one container; once with the 16 steps, once with the short profile. Decides the sandbox, the default step profile, the default cost limit and the tables of `execution` | Minutes and cost per task for both profiles, recorded in `todo.md` | 2 |
| M4 | One run end to end, thin | US-06.01 to 04, 07, 12 in their thinnest version: sandbox, template, orchestrator, one task after the other, live status with a link to try the result, ZIP | A run on the canteen contract ends with software that builds and passes its tests | 6 |
| M5 | Runs that survive | US-06.05, 06, 09, 10: error classes and fix loops, asking the client, stop, resume, limits, waiting for tokens | Each case shown with a stub and once live | 6 |
| M6 | Complete planning and settings | The remaining stories of E-01 to E-05 | Acceptance scenarios of `requirements.md` 11.1 for screens 0 to 8 | 8 |
| M7 | Delivery | US-06.11 and the rest of US-06.12: GitHub push per task, Render link, final report | A public repository and a deployed result from one run | 3 |
| M8 | Verification and release | E-07; a check of every Must requirement against a test; a clean-clone start on another machine; tag v1.0.0 | Reference run within the time target; the engine runs from GitHub without help | 4 |

About 42 sessions in total. The backlog of each domain is in `epics/E-01.md` to `E-07.md`; the story ids there stay as names of the parts.

## Until 20 November 2026 (assignment #1)

M0 and M1 are the must; M2 as far as the time reaches. Neither needs Docker (the tests and the application use the PostgreSQL installed on the machine) nor a key in a file (automated tests use a stub; for the live check the key is typed into the page and held for the session only).

## What Docker and the key are for

| Thing | Needed for | Needed from |
|---|---|---|
| A Claude API key | Every user of the product enters one on screen 1 (BR-08; a product for others cannot use a claude.ai login, CON-04). Development needs it only for the live check of a slice; the cheapest model is enough for that | The first live check of M1 |
| Docker | Running generated code apart from the machine and with a fixed toolchain (M3, M4), and starting the engine on another machine with one command (M8) | M3, second half |
| Claude Code on this machine | The first half of the build spike: it builds in a workspace folder with its own sandbox and the owner's existing sign-in, so the spike costs no API money | M3, first half |

## Risks

| Risk | Answer |
|---|---|
| The time and cost of one built task are unknown; the earlier architecture decision E-ADR-09 judged that 16 steps do not fit 4 hours | M3 measures both step profiles before anything else of the run is built |
| Privileged Docker-in-Docker for the sandbox | M3 builds in a workspace folder first, then in a single container with PostgreSQL; Docker-in-Docker only if the template's tests need it |
| The schemas of `execution` and `settings` were designed before any run existed (verdicts, member reports, audit findings per task) | They stay drafts in `skeleton/` until M3 has shown what a run produces; run events get a sequence number for SSE replay |
| AI answers are not deterministic and cost money | Every AI result is validated against its schema with one retry; automated tests use a stub; a cost limit per run with a default from M3 |
| One machine, one developer | One environment; `ci/run all-local` is the only gate; small slices |
| Nine template setups are a Must in `requirements.md` (TR-11) | The MVP has one (D-21); the requirement is changed when the Word master is updated |

## Why the earlier plan was replaced

- It asked for 10 setup stories and 10 business stories in 6 days, each through 16 stages with a verdict of every role, with a build time of about 3 hours a day.
- One person was Product Owner, approving Architect and the only one who merges, and nobody may merge their own work.
- The technology rule documents (about 11,000 lines) were generic templates without anything on Spring AI, SSE, long AI runs or the sandbox, partly against the chosen stack (a Web Component per domain in SvelteKit) and partly against taken decisions.
- The largest technical risk (sandbox, harness, the 4-hour run) was planned last and never measured.
