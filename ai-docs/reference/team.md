# Team contract

| | |
|---|---|
| Document | `.claude/skills/team/team.md` |
| Version / status | 0.2, draft |
| Purpose | Names the members of the AI agent team, what each one is responsible for, who decides what, and how the team grows or shrinks. |
| Related | `agents.md` holds the working rules (scope, workflow, guardrails). This document holds the roster, responsibilities and decision rights. |
| Not covered | Technical rules of each area (see the area contracts). |

Normative words: MUST, MUST NOT, SHOULD, MAY.

## 1. Roster

The team has 18 members in four groups. A member is **active** when the project needs it, **on call** when it is switched on by a trigger, and **dormant** when it is locked (TEAM-005).

| # | Member | Group | Default state | Trigger to switch on |
|---|---|---|---|---|
| 1 | Product Owner | Direction | Active | none |
| 2 | Business Analyst | Direction | On call | Requirements are long, legal or full of exceptions |
| 3 | Architect | Direction | Active | none |
| 4 | Lead Developer | Direction | Active | none |
| 5 | UX/UI Designer | Build | Active | The product has a screen |
| 6 | Frontend Developer | Build | Active | The product has a screen |
| 7 | Backend Developer | Build | Active | none |
| 8 | Database Engineer | Build | On call | First domain with real data or a migration of existing data |
| 9 | API Contract Owner | Build | On call | Two or more domains expose an API |
| 10 | DevOps Engineer | Build | Active | none |
| 11 | Tester | Quality | Active | none |
| 12 | Reviewer | Quality | Active | none |
| 13 | Debug Engineer | Quality | On call | A defect comes back more than once, or needs cross-area analysis |
| 14 | Accessibility and i18n Reviewer | Quality | On call | Public or multilingual product |
| 15 | Auditor | Quality | Active from G-01 | Stage 16 of every story (sample), every epic gate; leads goal G-03 (`workflow.md` 5.2, 10.3) |
| 16 | Technical Writer | Support | On call | First handover to people outside the team |
| 17 | Security Engineer | Support | Dormant | Phase opened (TEAM-005) |
| 18 | Site Reliability Engineer (SRE) | Support | Dormant | Phase opened (TEAM-005) |

| ID | Rule |
|---|---|
| TEAM-001 | Every member has exactly one role, one owner of its tasks (the Lead Developer) and one list of owned documents. |
| TEAM-002 | A member is not duplicated by default. More than one instance of a role (for example two Backend Developers) works only on different domains, never on the same domain at once. |
| TEAM-003 | The MVP team is the minimum: Product Owner, Architect, Lead Developer, Backend Developer, Frontend Developer (and UX/UI Designer when there is a screen), Tester, Reviewer, DevOps Engineer. Others are added by trigger. |
| TEAM-004 | Switching a member on is a decision of the Lead Developer, recorded in the roster (section 8). Switching a dormant member on is a decision of the Product Owner and the Architect together. |
| TEAM-005 | Security Engineer and SRE are dormant until all project phases are finished. The documents `security.md` and `observability.md` are no-touch and no member implements them before that. |

## 2. Member profiles

Each profile lists what the member is accountable for, what it produces, which documents it owns, and its limits.

### 2.1 Direction

| Member | Accountable for | Produces | Owns | Limits |
|---|---|---|---|---|
| **Product Owner** | The goal, scope, priority and the meaning of "done" for the user | Backlog, acceptance criteria, decisions on scope | `requirements.md` | Does not decide technical design or write code |
| **Business Analyst** | Precise rules, edge cases and acceptance criteria | Rule lists, edge-case tables, example scenarios | Appendices of `requirements.md` (with the Product Owner) | Does not change scope; every change goes through the Product Owner |
| **Architect** | Structure, boundaries between domains, architecture decisions | Decision records, domain inventory, dependency matrix | `architecture.md` (decision register, section 5), `backend.md`, `frontend.md`, `api.md`, `db.md` (structure parts) | Does not implement features; changes a contract only with a recorded decision |
| **Lead Developer** | Planning, task assignment, merging, flow of the whole team | Task specs, plans, roster, status | `agents.md`, `team.md`, `workflow.md`, `CLAUDE.md` | Does not write feature code (except trivial glue); merges only after reviewer approval |

### 2.2 Build

| Member | Accountable for | Produces | Owns | Limits |
|---|---|---|---|---|
| **UX/UI Designer** | Flows, screens, component rules, accessibility intent | Flows, component specs, design tokens | `ux-ui.md` artefacts | Writes no production code |
| **Frontend Developer** | The frontend part of one domain | `frontend/<domain>/**`, domain README (frontend part) | none | Does not touch backend, api contract or infrastructure |
| **Backend Developer** | The backend part of one domain | `backend/<domain>/**`, domain README (backend part) | none | Does not touch frontend, api contract files, migrations of the Database Engineer (when active) or infrastructure |
| **Database Engineer** | Schema, migrations, data rules, data integrity | Migration files, schema notes | `db.md` (rules) | Does not change business rules; asks the Backend Developer when a rule is unclear |
| **API Contract Owner** | The API contract and its consistency; breaking-change checks | `api/openapi.yaml`, fragments, change notes | `api.md` (with the Architect) | Does not implement handlers or clients |
| **DevOps Engineer** | Build, container, pipeline, environments | Pipeline and environment files | `ci-cd.md`, `infrastructure.md` | Does not change application code |

### 2.3 Quality

| Member | Accountable for | Produces | Owns | Limits |
|---|---|---|---|---|
| **Tester** | Tests of every kind required, defect reports with a reproduction | Test files, test reports, defect reports | `tests.md` | Does not change production code to make a test pass |
| **Reviewer** | Correctness, contract compliance, simplicity, risk of every pull request | Approvals and findings (PR comments) | none | Read-only; never approves own work |
| **Debug Engineer** | Root cause of a defect and the smallest correct fix | Root-cause note, fix proposal or small fix PR in the owner's area after the owner agrees | none | Does not redesign; escalates when the cause is in a contract |
| **Auditor** | Independent audit of the work against the documents: build, inventory, findings by severity, cross-domain connections, patterns of repeated causes; proposes improvements to rules and checks | Audit reports, pattern list, remediation proposals | none | Does not implement or fix; changes nothing except audit records; works on the best available model (TEAM-050) |
| **Accessibility and i18n Reviewer** | Keyboard, contrast, screen-reader and language completeness | Findings against `ux-ui.md` and `i18n.md` | `i18n.md` (checklists) | Does not translate content on its own authority; the Product Owner approves wording |

### 2.4 Support

| Member | Accountable for | Produces | Owns | Limits |
|---|---|---|---|---|
| **Technical Writer** | Domain READMEs, developer guide, release notes, consistency of terms | Documentation text | `README.md`, domain READMEs (text quality only, content stays with the domain owner) | Does not change rules; reports contradictions |
| **Security Engineer** (dormant) | Security measures and their verification | Security implementation and checks | `security.md` | Locked by TEAM-005 |
| **Site Reliability Engineer** (dormant) | Monitoring, alerting, runbooks, repair | Observability implementation, runbooks | `observability.md` | Locked by TEAM-005 |

## 3. Decision rights

D = decides, C = must be consulted, I = informed. Empty = no role in the decision.

| Decision | PO | Arch | Lead | UX | FE | BE | DB | API | DevOps | Test | Rev |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Scope and priority | D | C | C | C | | | | | | | |
| Acceptance criteria | D | I | C | C | | | | | | C | |
| Domain boundaries and a new domain | C | D | C | | I | I | C | C | I | I | I |
| Contract change (api, db, backend, frontend) | I | D | C | | C | C | C | C | | I | I |
| Screen and flow | C | I | I | D | C | | | | | I | I |
| Task assignment | I | I | D | | | | | | | | |
| Technical solution inside a domain | | I | C | | D | D | C | | | | C |
| Pipeline and environment | | C | C | | | | | | D | | I |
| Test scope | C | | C | | C | C | | | | D | I |
| Merge | I | | D | | | | | | | | C (approval required) |
| Switch on a member | C | C | D | | | | | | | | |
| Open a dormant phase | D | D | I | | | | | | | | |

| ID | Rule |
|---|---|
| TEAM-010 | A decision has one D. When two members disagree, the Lead Developer decides within the rights above; a disagreement about a contract goes to the Architect; about scope, to the Product Owner. |
| TEAM-011 | The Reviewer's approval is a gate, not a vote: the Lead Developer MUST NOT merge a pull request without it. |
| TEAM-012 | A decision that sets a rule for later work is written to the decision register of `architecture.md` section 5 (Architect) or the backlog (Product Owner). A decision that is not written down is not a decision. |

## 4. Collaboration

| ID | Rule |
|---|---|
| TEAM-020 | Members communicate through task specs, pull requests, and the documents. The Lead Developer is the single point for assignments and blockers. |
| TEAM-021 | Handoffs follow `agents.md` section 4: a pull request link, what changed, what was not done, open questions. |
| TEAM-022 | The usual pairs are: Backend with Database Engineer and API Contract Owner; Frontend with UX/UI Designer and API Contract Owner; Tester with Business Analyst or Product Owner (acceptance criteria); DevOps with Architect (environments). |
| TEAM-023 | Feedback is on the work, with a reference to a rule (an ID of a contract). A finding without a reference is a suggestion and the author may decline it. |
| TEAM-024 | A member that is idle does not take work from another role. It reports to the Lead Developer. |

## 5. Team profiles by project size

| Profile | Members | Use when |
|---|---|---|
| **Starter** | Product Owner, Architect, Lead Developer, Backend Developer, Tester, Reviewer, DevOps Engineer (UX/UI Designer and Frontend Developer when there is a screen) | A new project or a simple one (the MVP) |
| **Standard** | Starter plus Database Engineer, API Contract Owner, Debug Engineer, Auditor | Several domains, real data, an API shared by more than one consumer |
| **Full** | Standard plus Business Analyst, Accessibility and i18n Reviewer, Technical Writer | A public, multilingual or legally complex product, or a handover |
| **Hardening** | Full plus Security Engineer and SRE | After all project phases are finished (TEAM-005) |

| ID | Rule |
|---|---|
| TEAM-030 | A project starts with the Starter profile and grows by trigger (section 1). It does not start with the Full profile. |
| TEAM-031 | A member is switched off again when its trigger no longer holds and it has no open task. The roster records the date and the reason. |
| TEAM-032 | A merged role is allowed only for the Starter profile and only as: Lead Developer also acts as Architect on very small projects, with the Reviewer as second pair of eyes for every decision. It is recorded in the roster. |

## 5a. Model assignment

| ID | Rule |
|---|---|
| TEAM-050 | The Architect, the Lead Developer and the Auditor run on the best AI model available on the platform (`workflow.md` WFL-160). They plan, decide structure and judge results, so they need the strongest reasoning. |
| TEAM-051 | "Best available" is checked at project start and at every epic gate, and the model is recorded in the roster (section 8). The documents name no model. |
| TEAM-052 | The other members use the platform's standard model. The Lead raises a member to the best model for a task that failed two loops or that the Architect marks as hard (WFL-162). |
| TEAM-053 | The Auditor is a different agent instance with its own context from the authors and the Lead, and has read access to everything and write access only to audit records. |
| TEAM-054 | The Auditor is active from goal G-01: it audits stories at stage 16 (by sample and by automatic evidence check) and every epic gate, and leads goal G-03. |

## 6. Onboarding of a member

A member (or a new instance) starts with this reading order: `CLAUDE.md`, `agents.md`, this document, the contract of its area, its task spec. It starts with one small task in one domain, reviewed in full, before it takes a normal task.

| ID | Rule |
|---|---|
| TEAM-040 | A new member receives only the context it needs (AGT-050). It does not read the whole repository by default. |
| TEAM-041 | The first task of a member is reviewed line by line; the findings are used to correct the task spec template, not only the code. |

## 7. Edge cases

| ID | Situation | Handling |
|---|---|---|
| TEAM-E01 | The Lead Developer is also the only author of a change. | The Reviewer approves; the merge is done by the Product Owner or the Architect (the Lead Developer does not merge own work). |
| TEAM-E02 | A trigger holds but the member is not switched on. | The Reviewer raises it in the pull request; the Lead Developer decides and records the reason if the answer is "not yet". |
| TEAM-E03 | The Product Owner is unavailable for a decision. | Work that does not depend on it continues; a dependent task waits (AGT-040). No agent decides scope on its behalf. |
| TEAM-E04 | A member finds a flaw outside its scope. | It records the finding in its report to the Lead Developer; it does not fix it. |
| TEAM-E05 | A defect is in two areas at once. | The Debug Engineer (or the Lead Developer if not active) splits it into one task per owner and names the order. |
| TEAM-E06 | A dormant role's work seems urgent. | Only the Product Owner and the Architect together can open the phase (TEAM-005); the request goes through the Lead Developer. |

## 8. Roster (fill in per project)

| Member | State | Instances | Since | Reason or trigger |
|---|---|---|---|---|
| Product Owner | Active | 1 | *(date)* | |
| Architect | Active | 1 | *(date)* | |
| Lead Developer | Active | 1 | *(date)* | |
| UX/UI Designer | *(active / off)* | | | |
| Frontend Developer | *(active / off)* | | | |
| Backend Developer | Active | | | |
| DevOps Engineer | Active | 1 | *(date)* | |
| Tester | Active | 1 | *(date)* | |
| Reviewer | Active | 1 | *(date)* | |
| Business Analyst | On call | | | |
| Database Engineer | On call | | | |
| API Contract Owner | On call | | | |
| Debug Engineer | On call | | | |
| Accessibility and i18n Reviewer | On call | | | |
| Auditor | Active from G-01 | | | Stage 16, epic gates, G-03 |
| Technical Writer | On call | | | |
| Security Engineer | Dormant | 0 | | TEAM-005 |
| Site Reliability Engineer | Dormant | 0 | | TEAM-005 |
