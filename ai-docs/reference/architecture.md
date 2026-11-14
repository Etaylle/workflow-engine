# Architecture overview and decision register

| | |
|---|---|
| Document | `.claude/skills/architecture/architecture.md` |
| Version / status | 0.3, draft for review (0.3: ADR-032 to ADR-040 Claude only, session key, GitHub and Render of the project starter, no login, builds on the engine machine, questions during a run, 16 steps; BPMN model; 0.2: engine profile ADR-023 to ADR-031) |
| Purpose | The entry point of the project documents. A short summary of the architecture and of every technical decision, with references to the document that holds the detail. An agent reads this first, then only the documents of its own work. |
| Owner | Architect. Changes follow section 9. |
| Not covered | The detail. Every statement here is a summary; the referenced document is authoritative (section 2). |

Rule IDs are `ARC-nnn`. Decision IDs are `ADR-nnn`.

## 1. How to use this document

| You are | Read | Then |
|---|---|---|
| Any agent starting work | Section 2 (authority), section 3 (map), section 6 (invariants), section 10 (start checklist) | The documents of your area (3) |
| An agent starting a story | `workflow.md` 4 and 5, the story card, the area documents named in the card | `tests.md` for the checks of your area |
| The Architect | All sections; the decision register (section 5) | The documents that a decision touches |
| The Auditor | Sections 3, 6, 7, 8 | `audit.md` (when created) and `workflow.md` 10 |
| The Product Owner | Sections 4, 7, 8 | `workflow.md` 2 and 3 |

| ID | Rule |
|---|---|
| ARC-001 | `CLAUDE.md` (project rules for the agents) points here first. This document points to the rest. An agent does not load the whole repository or all documents by default (`agents.md` AGT-050). |
| ARC-002 | This document stays short. When a section grows past a page, the detail moves to the area document and a reference stays here. |

## 2. Authority and conflicts

| ID | Rule |
|---|---|
| ARC-010 | Each document is authoritative for its own area (the map, section 3). This document is authoritative only for the decision register (5), the invariants (6), the deferred areas (7) and the document plan (8). |
| ARC-011 | When two documents disagree, the order is: (1) the Product Owner's explicit decision, recorded; (2) `workflow.md` for process and gates; (3) the contract of the area concerned for the content of that area; (4) this document. An agent that finds a conflict stops, records it, and asks the Lead (`agents.md` stop rules). |
| ARC-012 | A requirement not found in any document is not invented. The agent asks, or records the gap in the story (`workflow.md` story card). |
| ARC-013 | Security and observability are outside every document's active scope until the dormant phase is opened (section 7). Rules that look like security but are correctness or hygiene stay active. |

## 3. The document map

| Document | Owns | Prefix | Read when |
|---|---|---|---|
| `architecture.md` (this) | Summary, decisions, invariants, document plan | ARC, ADR | Always first |
| `workflow.md` | Goals, epics, stories, the 16 stages, the loop, gates, rhythm, hotfix lane, decision records and document ownership | WFL | Planning or doing any story |
| `team.md` | The roster, responsibilities, decision rights | TEAM | Deciding who does or approves |
| `agents.md` | Agent rules: scope, context, guardrails, stop rules, model assignment | AGT | Starting as an agent |
| `ux-ui.md` | Design system, foundations, components, patterns, page templates, accessibility | UX | Designing or styling a screen |
| `frontend.md` | The frontend area: structure, domains, components, state, API integration, rendering, errors, unit tests | (section rules) | Writing frontend code |
| `api.md` | The API contract: resources, HTTP semantics, errors, collections, writes, versioning, the contract file | API | Defining or changing an operation |
| `backend.md` | The backend area: modular monolith, domains and layers, persistence, migrations, integration, events, dependable behaviour, clean code, build | BKD | Writing backend code |
| `db.md` | The database: modelling, keys, types, constraints, migrations, seeds, import, performance, tests | DBS | Changing schema, queries, seeds |
| `i18n.md` | Locales, messages, formatting, translation resource, translation workflow | I18N | Any user-visible text, date, number |
| `tests.md` | The test levels L1 to L7, tools, Gherkin (suggested), data, evidence, quality | TST | Writing or reviewing tests |
| `ci-cd.md` | The pipeline: commands, steps S01 to S24, containers, remote deployment, providers, evidence | CIC | Changing the pipeline or deploying |
| `infrastructure.md` | Environments, Git branches and promotion, hosts, containers, configuration, data and backups, dependability, growth path, Epic 0 stories | INF | Setting up or changing where it runs |
| `ai-docs/requirements.md` | The requirements of the engine: business, user, functional (incl. FR-AGT agents), quality, technical, interface and data requirements (ISO/IEC/IEEE 29148, IREB) | BR, BRU, UR, FR, QR, TR, IR | Splitting wishes into epics and stories |
| `ai-docs/ui-mockup/` | The 14 MVP screens as images, static pages and design source | | Before building or reviewing a screen |
| `ai-docs/bpmn/` | The BPMN 2.0 model of the engine: project lifecycle and build one task (`.bpmn`, `.png`, README) | | Any change to the engine's flow, states or external connections |
| `README.md` (in `ai-docs/`) | The index of this folder, the reading order and the product decisions on one page | | First, together with `CLAUDE.md` |
| `ai-docs/word/` | The Word masters: project concept, stakeholder summary, requirements specification, architecture (arc42). The Markdown files are what agents read | | A human reviews or presents |
| `ai-docs/software-elements.md` | The map from each screen element to component, API operation, backend class, table and AI agent; the skeleton files | | Building a slice of a screen |
| `ai-docs/engine-architecture.md` | The arc42 architecture of the engine itself: building blocks, runtime, deployment, AI layers, E-ADR-01 to E-ADR-23 | E-ADR | Any engine design work |
| `audit.md` | The audit method, checklists, report format | AUD | The Auditor's work |
| `CLAUDE.md` (repository root), `README.md` (written in Epic 0) | The agents' project rules; the human introduction | | Always (CLAUDE.md) |

Where documents meet, the rule IDs are the references. A reference such as `backend.md` BKD-MIG-009 is stable (the stability rule of each document applies).

## 4. The architecture at a glance

```text
                              Product Owner (wishes) ──► requirements ──► goals ─► epics ─► stories
                                                                                         │
 ux-ui design system ──► frontend ──── API contract (api) ────► backend domains ────► database
   (UX)                   (area)          (one file, versions)    (BKD, layers)         (DBS, schema/domain)
        i18n (messages) ─┘                                              │
                                                                         ▼
                 tests L1..L7 ◄── local pipeline (ci/run) ◄── same steps in the central pipeline ◄── Git (main + env pointers)
                                                                         │
                                                         local ─► ephemeral ─► dev ─► test ─► prod   (build once, promote)
```

| Layer of the picture | Summary | Detail |
|---|---|---|
| Product | One monorepo. Areas: `frontend/`, `backend/`, `api/`, `db/`, `ci/`, `infrastructure/`, `ai-docs/`. The product is cut into **semantic domains**; a domain is a vertical slice across design, frontend, API, backend and database | `backend.md` 3, `frontend.md` 5 |
| Backend | A modular monolith. One deployable, three process roles (`web`, `worker`, `migrate`). Per domain: `domain`, `application`, `adapters`, `interfaces`. Domains talk through published interfaces and events only | `backend.md` 3 to 8, 12 |
| Database | One engine (default PostgreSQL), one schema per domain, no cross-domain foreign keys, joins or views; forward-only migrations by expand and contract | `db.md` 2 to 8, 15 |
| API | One written contract (OpenAPI), contract first, generated clients, problem-detail errors, idempotent writes, versioned | `api.md` 3 to 12 |
| Frontend | An independent area, framework-neutral, with the same domains, a typed API client, explicit states, accessibility | `frontend.md`, `ux-ui.md` |
| Language | Messages and formats through one translation resource and the locale rules | `i18n.md` |
| Quality | The test pyramid L1 to L7; Gherkin scenarios are suggested, not required; the Auditor verifies | `tests.md`, `workflow.md` 6 |
| Delivery | One command locally (`ci/run all-local`); the same steps in the central pipeline; one image set per commit is promoted | `ci-cd.md` |
| Runtime | Containers on compose; layers or single container; local, dev, test, prod | `infrastructure.md` |
| Work | Four goals: technical setup, MVP requirements, audit and testing, CI/CD completion; an AI team with defined roles | `workflow.md`, `team.md`, `agents.md` |

### 4.1 The product process (BPMN 2.0)

What the engine does for one client project, and how its AI team builds one task. Files, element tables and modelling rules: `ai-docs/README.md` 8.

![Project lifecycle (BPMN 2.0 collaboration)](bpmn/workflow-engine-lifecycle.png)

![Build one task (called once per business task)](bpmn/build-one-task.png)

## 5. The decision register

Each decision has a status (accepted, deferred), the reason in one line, where it is detailed, and the trigger that reopens it. A decision changed later keeps its entry with the new status (section 9).

| ID | Decision | Why | Detail | Reopen when |
|---|---|---|---|---|
| ADR-001 | One monorepo with area folders | One history, one version of everything, simple promotion | `backend.md` 3, `infrastructure.md` 6 | A team or product split makes separate repositories necessary |
| ADR-002 | Cut the product by semantic domains; a domain is a vertical slice | Stories are vertical; a domain owns its files, tables and tests | `backend.md` 4, `frontend.md` 5, `workflow.md` 2 | A domain cannot be owned by one story flow |
| ADR-003 | Backend as a modular monolith, hexagonal layers per domain | The simplest deployable that keeps boundaries; extraction stays possible | `backend.md` 3, 5, 17, 18 | `backend.md` triggers BKD-TRG (separate deployable, broker, ...) |
| ADR-004 | One database engine, one schema per domain, no cross-domain joins | Boundaries enforced where data lives; each domain can leave later | `db.md` 3 | `db.md` triggers DX1 to DX14 |
| ADR-005 | Time-ordered UUID keys made by the application, `timestamptz` in UTC, `numeric` for money, text with CHECK instead of native enums | Portable, mergeable, predictable | `db.md` 6, 7 | A measured performance or engine reason |
| ADR-006 | Forward-only migrations compatible with the previous release (expand and contract); one migrate role before the new version | Rollback of containers without undoing the schema | `db.md` 15, `backend.md` 10, `ci-cd.md` CIC-132 | A migration that cannot be made compatible needs a story and a backup |
| ADR-007 | Contract-first REST API in one written file; problem-detail errors; versioning and deprecation rules | Frontend and backend work in parallel; breaking changes are visible | `api.md` | A second protocol is required by a consumer |
| ADR-008 | Frontend is an independent, framework-neutral area using the design system and the API client generated from the contract | Replaceable technology, enforced states and accessibility | `frontend.md`, `ux-ui.md` | The product needs another client kind (native, other channel) |
| ADR-009 | All user-visible text through one translation resource; locale rules fixed | No text in code; additional languages without code change | `i18n.md` | A language with new rules (direction, plurals) is added |
| ADR-010 | Test pyramid L1 to L7; scenarios in Gherkin as a suggestion; the Auditor leads the verification epic | Cheap checks first, few end-to-end tests, an independent view | `tests.md`, `workflow.md` 6, 10 | A level proves too slow or too flaky (`tests.md` 19) |
| ADR-011 | Four fixed goals; goal, epic, story, task hierarchy; 16 stages per story ending with audit and verify | One generic route for all requirements | `workflow.md` 2, 5 | A change of the process after a retrospective (`workflow.md` 12) |
| ADR-012 | An AI team with fixed roles; the best available model for Architect, Lead Developer and Auditor | Quality where decisions are made and checked | `team.md`, `agents.md` AGT-060 to AGT-063 | New roles by their switch-on conditions (`team.md`) |
| ADR-013 | Provider-neutral pipeline: commands `ci/run <command>`, thin provider wrappers | Any provider (GitHub, GitLab, Jenkins with Gitblit, others) runs the same steps | `ci-cd.md` 4, 11 | A second provider is needed (`ci-cd.md` X13) |
| ADR-014 | Build once, tag with the commit id, promote the same image set through dev, test, prod | What was tested is what runs | `ci-cd.md` 7, 10 | None expected |
| ADR-015 | One working trunk `dev` and the release line `main`, which changes only through a pull request from `dev` (D-16); short story branches, and pointer branches `env/dev`, `env/test`, `env/prod`; no environment branch holds its own code | One history for all environments; Git shows what runs where | `infrastructure.md` 5, `ci-cd.md` CIC-011, CIC-040 | A requirement for a long-lived release line (a supported old version) |
| ADR-016 | Containers on compose, over SSH to one host, layers or single container; the frontend container is the entry point | Smallest thing that deploys; portable to every host | `infrastructure.md` 8, 9, 11, `ci-cd.md` 9 | `infrastructure.md` IX1 to IX13 |
| ADR-017 | Recovery by backup and restore (logical dump, second copy, drilled), RPO 24 hours and RTO 4 hours as defaults | Data safety without a platform | `infrastructure.md` 12, 14, `db.md` DBS-ACC-004 | A shorter RPO, or data that cannot be recreated (IX5) |
| ADR-018 | No orchestrator and no message broker in the MVP; the outbox table and the job table carry messages and work | The cheapest correct solution; both have measurable triggers | `infrastructure.md` 15.3, 15.4, `backend.md` BKD-MVP-007 | Two of five criteria true (`infrastructure.md` 15) |
| ADR-019 | The MVP follows its own profile: a fixed core, listed simplifications, triggers to grow, a register of shortcuts | The product grows by need, not by guess | `backend.md` 2, `db.md` 2, `ci-cd.md` 3, `infrastructure.md` 3 | The shortcut's trigger fires |
| ADR-020 | Security is not created in the MVP: no `security.md`, no authentication, no roles, no hardening, no audit of security | Product decision: all other phases first | Section 7, `workflow.md` WFL-080, WFL-120 | The dormant phase is opened by the Product Owner and the Architect |
| ADR-021 | Observability is not created in the MVP: no `observability.md`, no metrics, tracing or alerting | The same decision as ADR-020 | Section 7, `workflow.md` WFL-080 | The same |
| ADR-022 | Correctness and hygiene rules stay active (validation, error handling, no secrets in code or Git, key-only deploy access, log limits) and are not security or observability work | Correctness is not optional | `workflow.md` WFL-081, `infrastructure.md` INF-DEF-002 | None |

**Engine profile (ADR-023 to ADR-031).** These decisions adapt the process of this document set to the *unattended runs* in which the workflow engine builds a client's project, and to the project templates. They do not change how the engine itself is developed: the engine follows ADR-001 to ADR-022 and the full workflow.

| ID | Decision | Why | Detail | Reopen when |
|---|---|---|---|---|
| ADR-023 | (Partly replaced by ADR-038: the run may ask the client.) Engine profile: unattended execution mode for engine runs. The confirmed contract snapshot is the Product Owner's approval; no question is asked during a run; a PO stand-in answers from the contract; open points are decided with the smallest reasonable option and logged in `DECISIONS.md`; a run stops only on a budget or by the client | A run of the engine has nobody to ask (AGT-074) | `engine-architecture.md` 5.5, 8.6, 8.15; `requirements.md` BRU-06, FR-AGT-09 | Applies to engine runs and templates only, not to the development of the engine. Reopen when runs need a human approval step |
| ADR-024 | (Replaced by ADR-039.) Engine profile: fast-lane story type for engine runs: Builder, Tester, Reviewer per feature slice, at most 2 fix loops; the Lead plans once per run; the Auditor once at the end | The 16 stages of `workflow.md` 5.2 do not fit a 4-hour run | `engine-architecture.md` 5.2, E-ADR-09 | Quality findings of reference runs show that a stage is missing |
| ADR-025 | Engine profile: Epic 0 of every generated project is pre-built per setup as a template; a run starts at G-02 | Saves the most error-prone hour of a run; every setup is tested in CI | `engine-architecture.md` 5.4, 8.11 | A setup cannot be kept green |
| ADR-026 | Engine profile: in generated projects, G-03 and G-04 are closed with a written reason (WFL-006): the final audit is the fast-lane audit, deployment is the client's next step | A run delivers a ZIP, not a release | `engine-architecture.md` 6.4 | Deployed previews are added (later) |
| ADR-027 | Engine profile: 4 hours is a target, not a hard stop: warning phase, overtime allowed and reported; pause on the cost limit or the ceiling (about 8 hours) | Finished work is worth more than a hard stop | `requirements.md` FR-TIME; `engine-architecture.md` 6.6, 8.8 | Clients need a hard deadline |
| ADR-028 | Engine profile: a run resumes up to 3 times from its last checkpoint after an engine restart; an interrupted step always restarts in a new agent session | Builds take hours; restarts happen; sessions cannot be trusted after an interruption | `requirements.md` FR-RES; `engine-architecture.md` 6.5 | Resume causes inconsistent results |
| ADR-029 | Engine profile: export by manifest and pattern cleaning, fresh Git history, customer pipeline mode and a final check; no rule IDs in code comments of generated projects | The client receives a clean project without the agent setup | `requirements.md` FR-EXP; `engine-architecture.md` 8.10 | A client wants the agent setup included |
| ADR-030 | Engine profile: a top-level folder `templates/` in the engine repository holds the template parts and the manifest (an approved new top-level folder, AGT-R05) | One version of engine and templates, built into the image | `engine-architecture.md` 5.4, 8.11, E-ADR-08 | Templates need their own release cycle |
| ADR-031 | Engine profile: Svelte custom elements in generated frontends render on the client side only | Avoids server-rendering differences between the three frontend setups | `engine-architecture.md` 8.11 | Server rendering becomes a requirement |

**Engine profile 0.3 (ADR-032 to ADR-039).** Decisions of 13 Nov 2026 for the MVP. The BPMN model of the engine (`ai-docs/README.md` 8) shows all of them.

| ID | Decision | Why | Detail | Reopen when |
|---|---|---|---|---|
| ADR-032 | AI only through Claude: the agent harness is Claude Code or opencode with an Anthropic model. No local models, no other provider in the MVP (replaces the provider choice of `requirements.md` FR-AI-07/08 and E-ADR-16) | One provider keeps the prompts, the model tiers and the tests small enough for 15 hours of build time | `requirements.md` 0.3 changes; `engine-architecture.md` 0.3 changes; `settings.provider_models` holds only `anthropic` | A second provider is asked for by a client |
| ADR-033 | The API key is kept for the session only: entered on screen 1, checked with one test call, held in engine memory for the browser session and for the runs started or continued in it; never written to the database, logs, files, the repository or the ZIP; entered again in every new session and after an engine restart (a paused run asks for it). The browser session is an anonymous session identifier that only keys this memory; it is not authentication and does not open the deferred security phase (ADR-020, `backend.md` BKD-DEF-001). The same holds for the GitHub sign-in of ADR-035: it authorises pushes to the starter's account, not access to the engine | No login and no secret store in the MVP: the safest key is a key that is never stored | `ai-docs/README.md` 8; `software-elements.md` 0.3; table `projects.project_api_keys` removed | A login and a secret store exist |
| ADR-034 | No login in the MVP: anyone who opens the engine sees all projects and can open, change and continue any of them | Single-team MVP; login is deferred with security (ADR-020) | Screen "All projects"; `audit/deferred-risks.md` | The engine is opened to people outside the team |
| ADR-035 | GitHub of the project starter: the person who starts a project signs in to GitHub at the start (device flow, scope `public_repo`); every finished task is committed and pushed to a public repository in that account, created on the first push; the token lives in engine memory for the session and the run only; without GitHub the result is the ZIP only | No engine-wide GitHub account and no client accounts are needed; open source is the MVP licence | `ai-docs/README.md` 8 level 1 and 2 (Task commit); `projects.github_connections` without token column | Private repositories or organisation accounts are needed |
| ADR-036 | Render.com of the project starter, optional: the engine adds a `render.yaml` Blueprint to the generated repository and shows a "Deploy to Render" link (`https://dashboard.render.com/blueprint/new?repo=<repository URL>`); the starter deploys in their own Render account; the engine keeps no Render key | Deployment stays the client's step; no second secret | Screen 8 and 10; `engine-architecture.md` 0.3 changes | The engine should deploy by itself |
| ADR-037 | Builds run on the machine where the engine runs (Docker sandbox: runner, Docker-in-Docker, workspace). The Claude API only answers prompts; the user's computer only shows the browser and receives the ZIP. The engine is not hosted on Render (Render does not allow privileged Docker) | Agents must run builds, containers and tests | `engine-architecture.md` 7; `ai-docs/README.md` 8 | A hosted runner with privileged containers is available |
| ADR-038 | The run may ask the client: when a defect is not fixable after two attempts and the best model, the development supervisor asks one question with short answer options (screen 12, "Human action required"); the client can always stop, which exports the ZIP of the last checkpoint. Replaces the "no question during a run" part of ADR-023 | A short question saves a failed run; the BPMN error and stop paths make it explicit | `ai-docs/README.md` 8 level 2 (Ask the client, Continue?); level 1 (Stop at any time) | Reference runs show that questions block runs |
| ADR-039 | Engine runs use the 16 development steps of `workflow.md` 5.2 (story types: 5.3) with the members of `team.md` (steps that do not apply are skipped and shown so), one business task after another. Replaces ADR-024 (fast lane) | The client sees the same, editable development workflow on screen 7 that the team uses | `ai-docs/README.md` 8 level 2; screens 6, 7, 11 | A 4-hour run is regularly exceeded by more than 50 % |
| ADR-040 | The engineering documents live in `ai-docs/` (not `docs/`): the requirements, the engine architecture, the BPMN model, the UI mockup, the software element map, the code skeleton (`ai-docs/skeleton/`) and the Word masters in `ai-docs/word/`. `ai-docs/README.md` is the index and the one-page summary of the product decisions. Amended 13 November 2026 by the Product Owner: the 13 rule documents (this one, `workflow.md`, `team.md`, the area contracts and `audit.md`) are Claude Code skills in `.claude/skills/<area>/<area>.md`, so that agents load them by relevance; `agents.md` is at the repository root | One place for people and agents; the name says who reads it first | `ai-docs/README.md`; `CLAUDE.md` 5 | A published documentation site is needed |
| ADR-041 | All environments run on the local machine with Docker until the presentation (decision of the Product Owner and approving Architect, 13 November 2026, WFL-173): `local` (`ci/run all-local`) and `dev`, `test`, `prod` as separate compose projects on the same machine, each with its own volume, env file and ports (INF-HST-003, INF-ENV-004). G-01 closes with local containers only. Test and prod exist from Epic 0 instead of G-04 (deviation from INF-EP0-002): every merged story is deployed to dev, every finished epic is promoted dev → test → prod with the PO's approval (CIC-140, CIC-142). The deployment commands run on the local machine; a central pipeline only checks pull requests when the engine has a GitHub repository. No remote host; a later host name may be `amberneurons-engine` (.com, .at or .ai), names and certificates stay dormant (INF-NET-006) | The engine is presented and tried out on the local machines of students; no remote budget; the full promotion path is still exercised | `ai-docs/plan.md`; `ai-docs/epics/E-00.md` (US-00.10, US-00.14); `infrastructure.md` 3.2, 4, 5.3 | A remote host is wanted: its first deployment becomes the first story of G-04 (WFL-173) |

## 6. Invariants every agent keeps

| ID | Invariant | Where |
|---|---|---|
| ARC-020 | Work in a domain; use another domain only through its published interface, events or API operations. No cross-domain table access. | `backend.md` BKD-SLC, `db.md` DBS-ARC |
| ARC-021 | The API contract file is the single source of the API. Change it first; generate from it; breaking changes follow the versioning rules. | `api.md` 3, 12 |
| ARC-022 | No text, date or number format in code; use the translation resource and locale rules. | `i18n.md` |
| ARC-023 | Every screen state (loading, empty, error, partial) is designed and implemented; accessibility is a requirement, not a polish step. | `ux-ui.md`, `frontend.md` 14 |
| ARC-024 | Time is UTC in storage and transport; identifiers are generated by the application; money is exact; the database enforces what must always hold. | `db.md` 6 to 8 |
| ARC-025 | Writes are idempotent where the semantics allow; every wait is bounded; every dependency can fail and the failure is defined. | `backend.md` 13, `api.md` 10 |
| ARC-026 | Configuration comes from the environment; nothing environment-specific is in an image or in a branch; secrets are never in Git, logs, images or reports. | `infrastructure.md` 10, `ci-cd.md` 8 |
| ARC-027 | Migrations are forward-only and compatible with the previous release; seeds run twice with the same result. | `db.md` 15, 16, `workflow.md` WFL-182 |
| ARC-028 | A story is done only when the whole local pipeline is green, the team verified it and the evidence block is complete. | `workflow.md` 5, `ci-cd.md` 13 |
| ARC-029 | Build once and promote the same build id. A failed build id is never promoted. | `ci-cd.md` CIC-001, CIC-141 |
| ARC-030 | Do not create what the MVP profile defers. Grow only on a trigger and record it. | Section 5 ADR-019 |
| ARC-031 | Do not touch security and observability (section 7). | `workflow.md` WFL-080 |
| ARC-032 | An agent never holds production credentials; humans create and place them. | `infrastructure.md` INF-PRN-010 |
| ARC-033 | Destructive operations on a remote host (volume removal, database drop) are never done by an agent or a pipeline. | `infrastructure.md` INF-R05 |

## 7. Deferred areas

| Area | Decision | State | Trigger to open |
|---|---|---|---|
| Security: `security.md` | Not created in the MVP. No authentication, authorisation, roles, hardening, TLS policy, scans or security audit. Requirements may be recorded in `requirements.md` marked "deferred" | Dormant (ADR-020) 
| Observability: `observability.md` | Not created in the MVP. No metrics, tracing, dashboards or alerting. Health endpoints, container logs with limits, the run record and the incident record remain | Dormant (ADR-021) | The same |

| ID | Rule |
|---|---|
| ARC-040 | Until the dormant phase is opened, no goal, epic or story, and no agent, implements anything in these areas. A request is recorded for that phase and not built (`workflow.md` WFL-080, `infrastructure.md` INF-DEF-001). |
| ARC-041 | The places reserved for them already exist: the pipeline's deferred steps (`ci-cd.md` 15), the entry point where TLS and names go (`infrastructure.md` INF-NET-008), the secret files (`infrastructure.md` INF-CFG-004), the area contracts' deferred items (`backend.md` 17, `frontend.md` Annex E, `api.md` 15, `tests.md` 21). |
| ARC-042 | When the phase is opened: this register gets new decisions, `security.md` and `observability.md` are created with the same structure as the other contracts, and the extension triggers `ci-cd.md` X14 and `infrastructure.md` IX13 are executed as stories. |
| ARC-043 | The risks the Product Owner accepted by this deferral (public plain HTTP environments, no access control, demo data only) are listed by the Auditor at each epic gate (`infrastructure.md` INF-DEF-003). |

## 8. The document plan

| Document | Status | Next step |
|---|---|---|
| `workflow.md` | 1.1, final | None |
| `ci-cd.md` | 1.1, draft for review | Review |
| `infrastructure.md` | 0.2, draft for review (0.2: engine profile ADR-023 to ADR-031; document map and plan updated) | Review; its section 17 is the detailed list of Epic 0 stories (the list in `workflow.md` 9.3 is an example) |
| `db.md` | 0.2, draft for review (0.2: engine profile ADR-023 to ADR-031; document map and plan updated) | Review |
| `backend.md` | 0.5, draft | Review |
| `tests.md` | 0.6, draft | Review |
| `agents.md`, `team.md` | 0.2, draft | Review, then mark 1.0 |
| `api.md`, `frontend.md`, `ux-ui.md`, `i18n.md` | Drafts | Review |
| `architecture.md` | 0.1, this document | Review |
| `ai-docs/requirements.md` | 0.5, proposed for review | Review with the Product Owner; generated from the requirements specification (Word) |
| `ai-docs/software-elements.md` | 0.1, draft for review | Review with the Architect; fixes the API paths in `api/openapi.yaml` |
| `ai-docs/engine-architecture.md` | 0.4, draft for review | Review with the Architect; generated from the arc42 document (Word) |
| `audit.md` | Draft | Review before the first epic gate |
| `CLAUDE.md` | Created; section 9 filled | Kept current by the Lead (US-00.13 of `infrastructure.md` 17); it points here |
| `README.md` | Not created | Written in Epic 0 for humans |
| `security.md`, `observability.md` | Not created by decision | Section 7 |

| ID | Rule |
|---|---|
| ARC-050 | A document is created when its work starts, not before: `requirements.md` at G-02, `audit.md` before the first epic gate. |
| ARC-051 | Every document has an owner (`workflow.md` 12.6). A document without an owner is reported by the Auditor. |
| ARC-052 | When a document is added, renamed or removed, section 3 and section 8 are changed in the same pull request, together with the references in the other documents. |

## 9. Decisions and changes

| ID | Rule |
|---|---|
| ARC-060 | A decision is made by the Architect with the person who owns the area, and with the Product Owner where it touches scope, cost, risk or a deferral. The decision is written into the register as a new `ADR-nnn` row: decision, reason, detail, reopen trigger. Rows are not deleted; a changed decision gets a new row and the old row is marked "replaced by ADR-nnn". |
| ARC-061 | Every extension trigger of an area document (`backend.md` BKD-TRG, `db.md` DX, `ci-cd.md` X, `infrastructure.md` IX) that fires is recorded here as a decision before the story starts. |
| ARC-062 | A change that touches several documents is one story; the documents change together; this document is updated last. |
| ARC-063 | The register records decisions only. Rules stay in the area documents. |
| ARC-064 | A rule that an agent needs often and that is stable (an invariant) is moved up to section 6 only when two or more areas depend on it. |

Decision template:

```text
ADR-nnn  <title>        Status: accepted | deferred | replaced by ADR-nnn
Context:  what forced the decision (one or two lines)
Decision: what is decided
Reason:   why this and not the alternatives (name them)
Detail:   document and section
Reopen when: the measurable trigger
Decided by / date:
```

## 10. Start checklist for an agent

| # | Step |
|---|---|
| 1 | Read `CLAUDE.md`, then this document sections 2, 6 and 7 |
| 2 | Read your role in `team.md` and your rules in `agents.md` |
| 3 | Read the story card and the context packet (`workflow.md` 4, WFL-224) |
| 4 | Read only the area documents the card names (section 3); find rules by ID, not by reading the whole document |
| 5 | Start the local environment with one command (`ci/run env-up layers`, `infrastructure.md` 7) |
| 6 | Work the stages of `workflow.md` 5.2; run `ci/run all-local` before the verification round |
| 7 | If a rule is missing or documents disagree, stop and record it (ARC-011, ARC-012) |
| 8 | If the task touches security or observability, stop and record it for the dormant phase (ARC-040) |
