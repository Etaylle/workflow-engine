# Audit contract

| | |
|---|---|
| Document | `.claude/skills/audit/audit.md` |
| Version / status | 0.1, draft for review |
| Purpose | Defines how the work is audited: at which points of the workflow, from which three views (technical, tests, business and product), with which checks, how findings are recorded and closed, and how an audit decides whether a story, an epic, a goal or a release may pass. It is the method of the Auditor. |
| Related | [`workflow.md`](workflow.md) (stage 16, epic gates 11, goal G-03 10.3, learning 7.5, audit report 17.5), [`team.md`](team.md) and [`agents.md`](agents.md) (the Auditor's role and limits), [`architecture.md`](architecture.md), [`tests.md`](tests.md), [`ci-cd.md`](ci-cd.md), [`infrastructure.md`](infrastructure.md), [`db.md`](db.md), [`backend.md`](backend.md), [`frontend.md`](frontend.md), [`api.md`](api.md), [`ux-ui.md`](ux-ui.md), [`i18n.md`](i18n.md). |
| Owner | Auditor (`workflow.md` 12.6), with the Architect and the Lead Developer for changes. |
| Not covered | A security audit and an observability audit: they are no-touch until the dormant phase is opened (`workflow.md` WFL-120, `architecture.md` 7). This document lists what the Auditor records about them instead (section 13). |

Normative words: MUST, MUST NOT, SHOULD, MAY. Rule IDs are `AUD-nnn`; checklist items `AUD-Tnn` (technical), `AUD-Qnn` (tests), `AUD-Bnn` (business and product); edge cases `AUD-Enn`; restrictions `AUD-Rnn`; parameters `AUnn`; audit points `AP1` to `AP6`; extension triggers `AXn`.

## Contents and version

| Section | Content |
|---|---|
| 1 | Scope and definitions |
| 2 | Principles |
| 3 | The MVP profile |
| 4 | Audit points: when an audit happens |
| 5 | The three lenses and their checklists |
| 6 | The method of an audit |
| 7 | Findings: severity, record, lifecycle |
| 8 | Cross-domain verification |
| 9 | Decisions and gates |
| 10 | Patterns and learning |
| 11 | Independence, access and records |
| 12 | Reports and templates |
| 13 | Deferred areas: what the Auditor records |
| 14 | Edge cases |
| 15 | Restrictions |
| 16 | Parameters |
| 17 | Sources and limits |

Version history: 0.1 first version: six audit points, three lenses with 52 checklist items, a method in eight steps, severities and gates, records under `ai-docs/audit/`.

## 1. Scope and definitions

| Term | Meaning |
|---|---|
| Audit | An independent check of work against the documents, made by running things and reading evidence, not by trusting a claim. It ends with a recorded decision. |
| Audit point | A place in the workflow where an audit happens (section 4): the end of a story, an epic, a goal, a release, a hotfix, or on request. |
| Lens | One view of the product: **technical** (T), **tests** (Q), **business and product** (B). Every audit looks through all three. |
| Checklist item | One question with a way to check it, a source rule and the audit points where it applies (section 5). |
| Evidence | A result that anyone can check again: a command and its output, a run id, a file and line, a test report, a screen on the deployed build. |
| Finding | A gap between what the documents require and what exists, with evidence and a severity (section 7). |
| Accepted finding | A finding that stays open on purpose, accepted in writing by the Product Owner and the Architect, with a reason and a date (WFL-260). |
| Pattern | A cause that repeats in two or more findings (`workflow.md` 7.5). |
| Deferred risk | A risk that the Product Owner accepted by deferring security or observability, or by an MVP shortcut (section 13). |
| Independence | The Auditor did not build, review or accept the work it audits, and has its own context (TEAM-053). |

Basis codes used in the checklists: **WFL** workflow rule, **ARC** invariant, **AGT** agent rule, **BKD**, **DBS**, **API**, **UX**, **I18N**, **TST**, **CIC**, **INF** the rule of the area contract that the item checks.

## 2. Principles

| ID | Principle | Rule |
|---|---|---|
| AUD-001 | Independent | The Auditor works apart from the authors, the Reviewer and the Lead. It reads everything and changes nothing except audit records (`workflow.md` WFL-117). Nothing is audited by the agent that made it (`workflow.md` WFL-202). |
| AUD-002 | Evidence over claim | A status, a report or a verdict is a claim until the Auditor has seen the evidence. Where the Auditor's report and a status differ, the Auditor's report prevails (`workflow.md` WFL-117: the Auditor works independently of the authors). |
| AUD-003 | Three lenses, every time | Every audit looks at the technical, the tests and the business view. An audit that covers one lens is incomplete and cannot pass a gate (AUD-040). |
| AUD-004 | Run it, do not only read it | The Auditor builds from a clean checkout, runs the pipeline commands, and checks the deployed build, before it judges (AUD-030). |
| AUD-005 | Proportionate | Depth follows risk and the audit point: a sample at the end of a story, full at a gate. The pipeline checks the evidence of every story; the Auditor goes deep by sample (`workflow.md` WFL-057). |
| AUD-006 | Rules by ID | A finding names the rule or requirement it breaks (`team.md` TEAM-023). A finding without a reference is an observation. |
| AUD-007 | Learning, not blame | Findings feed patterns and rule changes (`workflow.md` 7.5), not judgement of agents (WFL-251). |
| AUD-008 | Repeatable | An audit can be repeated by another Auditor from the record: commands, build id, sample and checklist are written down. |
| AUD-009 | Security and observability are out | The Auditor does not audit what is deferred; it records the deferral and the accepted risks (section 13). |
| AUD-010 | Simple first | The checklist is the minimum. Items are added only when a finding showed a gap (AUD-081); an unused item is removed. |

## 3. The MVP profile

The audit of the MVP is light in tools and strict in independence and evidence.

### 3.1 Fixed core

| # | Element | Rule |
|---|---|---|
| 1 | An independent Auditor on the best available model | AUD-001, `team.md` TEAM-050 |
| 2 | The three lenses at every audit point | AUD-003, AUD-040 |
| 3 | A run from a clean checkout before any judgement | AUD-030 |
| 4 | Findings with severity, evidence and rule reference | AUD-060 to AUD-068 |
| 5 | A written decision at every gate with PO and Architect acceptance of open findings | AUD-070 to AUD-076 |
| 6 | Records kept in Git under `ai-docs/audit/` | AUD-090 |
| 7 | The deferred-risk register | AUD-100 |

### 3.2 Simplifications at the MVP

| Topic | Full form | MVP profile |
|---|---|---|
| Story audit | Every story in depth | Evidence of every story checked by the pipeline; in depth by sample (AU01) |
| Checklists | Automated checks for every item | Checked by command where a command exists; the rest by the Auditor's inspection |
| Reports | Generated dashboards | Markdown files in `ai-docs/audit/reports/` |
| Test strength | Mutation testing | Sampled probes on a scratch copy only when a trigger fires (AX3) |
| Schedule | A scheduled audit run | None until G-04 or an incident (AX2) |
| Tooling | An audit dashboard | None (AX4) |

### 3.3 Extension triggers

| # | Extension | Trigger |
|---|---|---|
| AX1 | Automatic evidence check of every story in the pipeline | More than one story a day, or an evidence gap found at an epic gate (`ci-cd.md` X7) |
| AX2 | A scheduled audit run against the first remote environment | Goal G-04, or a defect that a scheduled run would have found (`ci-cd.md` X6) |
| AX3 | Mutation probes on a scratch copy to test the strength of tests | Tests passed while a defect existed, or a pattern of weak tests (AUD-Q13) |
| AX4 | An audit dashboard | More than 20 reports or findings are hard to read |
| AX5 | Security and observability audits | The dormant phase is opened (section 13) |
| AX6 | An external or human auditor in addition | A requirement of the owner, a regulation or a customer |

| ID | Rule |
|---|---|
| AUD-020 | A simplification is registered in `ai-docs/README.md` section 9 with the date, reason and trigger. The Auditor reviews its own register at each goal gate. |
| AUD-021 | An extension is a story with its trigger and a decision of the Architect, recorded in `architecture.md` (ARC-061). |

## 4. Audit points: when an audit happens

| Point | When | Scope | Depth | Result | Effect |
|---|---|---|---|---|---|
| **AP1 Story** | Stage 16 of every story | The story: its evidence, its criteria, its deployed result | Evidence of every story by the pipeline; in depth by sample (AU01) | A story audit record (12.1) | The story is Done, or returns to the owner of the cause (WFL-056) |
| **AP2 Epic gate** | When the Lead calls the gate of an epic | All stories of the epic, the integrated build, the epic's requirements | Full for the epic; the items marked E | An epic audit report (12.2) | The epic is Closed, or stays in Gate state (WFL-077) |
| **AP3 Goal gate** | When the last epic of a goal is closed | The goal: G-01 setup readiness, G-02 MVP completeness, G-03 integration and verification, G-04 delivery path | Full; the items marked G; G-03 adds section 8 | A goal audit report (12.2) | The goal is closed, or reopened |
| **AP4 Release** | Before a promotion to test (light) and to prod (full) | The build id to be promoted, its evidence, the target environment | The items marked R | A release audit record (12.3) | The Product Owner may approve the promotion, or not |
| **AP5 Hotfix** | After a hotfix is deployed (`workflow.md` 7.8) | The fix, its reproducing test, the follow-up work | The items marked H | A short audit record | The follow-up story has a date (WFL-243) |
| **AP6 On request** | The Product Owner, the Architect or the Lead asks, or the dormant phase is about to open | The scope the request names | As named | A report in the format of 12.2 | As named |

| ID | Rule |
|---|---|
| AUD-030 | Every audit starts with a run on a clean checkout of the build under audit, with the commands of `ci-cd.md` 4.1 (`ci/run all-local` and, where an environment exists, `ci/run smoke <env>`). The commands, the build id and the results are written into the report. |
| AUD-031 | The audit of a goal gate includes the audits of its epics: it checks that each epic report exists, that its findings are closed or accepted, and that the integrated state of the goal still holds. |
| AUD-032 | G-01 is audited at its gate as the setup readiness of `workflow.md` 9.4 and the Epic 0 gate (WFL-075); G-02 as the completeness of the MVP scope; G-03 as the verification epic (`workflow.md` 10.3); G-04 as the delivery path in all environments (`workflow.md` 10.4). |
| AUD-033 | A promotion to prod is not approved by the Product Owner without a release audit record for the same build id (AP4). A build id that changed after the record is a new build and needs a new record. |
| AUD-034 | The audit points are fixed by the workflow. A project may add a point through a story (for example an audit before each demo), never remove one. |
| AUD-035 | The audit of a story at AP1 does not wait for the end of the epic. It happens when the story is merged and deployed, because a late audit finds many problems at once (`workflow.md` 19). |
| AUD-036 | Audit stories are stories of the type "audit" (`workflow.md` WFL-210). Their persona is the Product Owner, an operator or a new developer. |
| AUD-037 | The audit point times are tracked in the process metrics: lead time of stage 16 and the number of loops it caused (`workflow.md` 7.9). |

## 5. The three lenses and their checklists

Each item has an ID, the question, how to check it, the rule or document it comes from, and where it applies: **S** story (AP1, by sample unless it is marked "all"), **E** epic gate, **G** goal gate, **R** release, **H** hotfix. "Command" means that a command of the pipeline shows the answer; otherwise the Auditor inspects and writes the evidence.

### 5.1 Technical lens: is it built right?

| ID | Question | How to check | Source | Applies |
|---|---|---|---|---|
| AUD-T01 | Does the product build, start and pass its checks from a clean checkout with one command? | Command `ci/run all-local` on a clean checkout | CIC-031, WFL-071 | S E G R H |
| AUD-T02 | Are the domain borders kept: no import across domains, no cross-domain table access or foreign key, only published interfaces and events? | The architecture test; the dependency matrix; schema inspection | ARC-020, BKD-SLC, DBS-ARC | S E G |
| AUD-T03 | Does the written API contract equal the running API, and does the frontend client equal the contract? | Contract tests (L4), contract diff, generated client check | ARC-021, API | S E G R |
| AUD-T04 | Do migrations run from empty and from the previous release, are they compatible with the previous build, and do seeds run twice with the same result? | Commands `migrate`, `seed` twice; upgrade-path run | ARC-027, DBS-MIG, CIC-259 | S E G R |
| AUD-T05 | Is each table owned by exactly one domain, with constraints that enforce what must always hold? | Schema inspection against `db.md` | DBS-ARC, DBS-CON | E G |
| AUD-T06 | Is the dependency register current, and is the package budget kept? | The pipeline check; the register | BKD 16.1, WFL 8 | S E G |
| AUD-T07 | Is configuration only from the environment, every variable listed, nothing environment-specific in an image or branch, no secret in code, images, logs or reports? | Command `check`; image inspection; log and report scan | ARC-026, INF-CFG, CIC-111 | S E G R |
| AUD-T08 | Is failure behaviour defined and implemented: bounded waits, bounded retries, idempotent writes, failures typed, degraded paths tested? | Domain README against code; failure tests | ARC-025, BKD-DEP | S E G |
| AUD-T09 | Does the frontend implement all states, use the translation resource and the generated client, and follow the design system? | Component and integration tests; code inspection | ARC-022, ARC-023, FE, UX | S E G |
| AUD-T10 | Does the pipeline deploy, verify and roll back as specified, in every environment that exists? | Run record; command `smoke <env>`; the rollback test | CIC-124, CIC-143 | E G R |
| AUD-T11 | Do the environments match `infrastructure.md`: same image, only configuration differs, no manual change on a host, pointers equal the deployed build ids? | Deployment records; `releases.log`; the pointer branches | INF-PRN-003, INF-CFG-007, INF-BRN-016 | E G R |
| AUD-T12 | Is the last restore drill recorded and within its interval, and does the backup run? | The drill record; backup log | INF-DAT-010, INF-DAT-014 | G R |
| AUD-T13 | Is every shortcut registered with a reason and a trigger, and is the register current in each area? | `backend`, `db`, `ci/`, `infrastructure` registers | INF-MVP-001, CIC-250, DBS-MVP | E G |
| AUD-T14 | Do the domain READMEs, API fragments, decision records and documents equal the code? | Inspection; document diff against code | WFL 8, ARC-063 | S E G |
| AUD-T15 | Is the code simple and clean: no dead code, no speculative generality, no copied logic, no shared dumping ground? | Review of the sample; the checks of the pipeline | BKD 15, BKD-SLC | S E G |
| AUD-T16 | Was nothing built that the MVP defers: no security, no monitoring, no extension without a recorded trigger? | Inspection of the diff and the registers | WFL-120, ARC-030, ARC-031 | S E G R |
| AUD-T17 | Are the performance and size budgets of the contracts kept (page weight, query plans, request deadlines)? | Build budgets; query checks; load figures if required | UX 11, DBS-PRF, BKD-DEP-004 | E G R |
| AUD-T18 | Are the contracts changed only by a recorded decision? | Decision records against diffs of contract files | AGT-R10, WFL-014 | S E G |
| AUD-T19 | Does the data layer follow `db.md`: keys, time in UTC, exact money, named constraints, indexes for the queries in use? | Schema and migration inspection | DBS | S E G |
| AUD-T20 | Is the clean-start acceptance possible: a person who did not build the product starts it from a clean checkout with one command and runs the journeys? | A fresh run by the Auditor in a new workspace | WFL-115 | G |

### 5.2 Tests lens: is it proven?

| ID | Question | How to check | Source | Applies |
|---|---|---|---|---|
| AUD-Q01 | Does every acceptance criterion and every requirement of the scope map to at least one passing test (traceability without gaps)? | The traceability matrix; the evidence block | WFL-040, TST 17 | S (all) E G R |
| AUD-Q02 | Is the last full run green on the merged state, and does the Auditor's fresh run give the same result? | The run record; command `all-local` | WFL-057, WFL-060 | S E G R H |
| AUD-Q03 | Does the suite follow the pyramid (many unit, fewer integration, few end-to-end)? | Counts per level from the reports | TST 3, WFL-042 | E G |
| AUD-Q04 | Is any test disabled, skipped or quarantined without an owner and an end date? | Test reports; the quarantine list | AGT-044, WFL-143 | S (all) E G |
| AUD-Q05 | Do the tests check behaviour and cover the error paths: the happy path, the wrong path, broken data and the unexpected event? | The sample of tests read against the criteria | TST 22, WFL-041 | S E G |
| AUD-Q06 | For a defect story: did a test reproduce the defect before the fix, and does it stay? | The commit order; the test | WFL-212, WFL-092 | S (all defects) H |
| AUD-Q07 | Does every API operation have a contract test, including the error responses? | The L4 reports against the contract | TST 9, API | S E G |
| AUD-Q08 | Are tests isolated and repeatable: data from seeds, no order dependence, no fixed ports, no flaky test? | Two runs in a row; the flaky list | TST 12, TST 19, CIC-253 | E G |
| AUD-Q09 | Is coverage per change reported, and does the gate of `tests.md` hold? | The coverage report | TST 17 | S E G |
| AUD-Q10 | Does every cross-domain business case have an end-to-end scenario that starts from seeded data and uses only published surfaces? | The scenario list against the business cases | WFL-113 | E G |
| AUD-Q11 | Do accessibility and visual checks run on the screens that exist? | The reports of L2, L5 and L6 | TST 14 | S E G |
| AUD-Q12 | Do non-functional tests exist for each non-functional requirement (performance, endurance, language, retention)? | The L7 reports against the requirements | WFL-016, TST 15 | E G R |
| AUD-Q13 | Would the tests fail if the code were wrong? | A probe on a scratch copy (only on trigger AX3); otherwise inspection of assertions | TST | E G (on trigger) |
| AUD-Q14 | Is the evidence stored and complete: run record, reports, screenshots, verdicts? | `ci/out/run-record.json`; the evidence block | CIC-247, WFL 17.3 | S (all) E G R |
| AUD-Q15 | Does the smoke check pass on every deployed environment and show the build id of the deployment? | `ci/run smoke <env>` | CIC S15, WFL-183 | E G R H |
| AUD-Q16 | Is the test code clean, named by behaviour, with stable selectors and without copied logic? | Review of the sample | TST 13, TST 20 | S E G |

### 5.3 Business and product lens: is it the right thing, and does it work for the user?

| ID | Question | How to check | Source | Applies |
|---|---|---|---|---|
| AUD-B01 | Is every requirement marked MUST of the audited scope Done and demonstrated on the deployed build? | The requirement list against the demo run | WFL-011 | S E G R |
| AUD-B02 | Were the acceptance criteria demonstrated as the Product Owner wrote them, and is the acceptance recorded with name and date? | The story record; the acceptance entry | WFL-260, stage 13 | S (all) E |
| AUD-B03 | Does the result deliver the "so that" benefit for the persona of the story, not only the letter of the criteria? | The Auditor uses the feature as the persona on the deployed build | WFL 4.1 | S E |
| AUD-B04 | Was nothing built that has no story, and nothing that the MVP scope excludes? | The diff against the story and the scope list | WFL-226, WFL-090 | S E G |
| AUD-B05 | Are the business rules and edge cases of the requirement covered by criteria and by behaviour (the Business Analyst's list)? | The edge-case list against tests and the running product | WFL-012, WFL 4 | S E G |
| AUD-B06 | Does each business case work from start to end through the screens a user uses, and is the data created in one domain visible and usable where the case needs it? | The cross-domain journeys run on the deployed build | WFL-113 | E G R |
| AUD-B07 | Are the words consistent with the product's terms, are all user-visible texts approved by the Product Owner, and are all supported languages complete? | The translation resource; the screens in each language | I18N, UX 9 | S E G R |
| AUD-B08 | Can a user complete the main tasks without help; are errors and empty states understandable; is the product usable with keyboard and screen reader at the level of the requirements? | Walkthrough of the tasks on the deployed build | UX, ARC-023 | S E G |
| AUD-B09 | Are the non-functional business needs met (languages, data retention, response time, availability target) as agreed? | The agreed values against measured values | WFL-016, INF-DPD-002 | E G R |
| AUD-B10 | Are open decisions and assumptions listed with owner and due point, and does no Ready story depend on an open decision? | The decision list | WFL-013 | E G |
| AUD-B11 | Are the deferred items and accepted risks listed and acknowledged by the Product Owner (security and observability deferral, plain HTTP environments, demo data only)? | The deferred-risk register | INF-DEF-003, AUD-100 | E G R |
| AUD-B12 | Is the release ready for the user: release notes, known issues, rollback ready, the availability target agreed, the Product Owner's approval recorded? | The release record; `rollback.md` run | CIC-142, INF-DPD-002 | R |
| AUD-B13 | Is every requirement traced to a story and every story to a requirement; are there no orphans? | The trace list | WFL-011 | E G |
| AUD-B14 | Is demo and seed data realistic for the business cases and free of real personal data? | Seed data inspection | INF-DAT-015, DBS-SED | E G R |
| AUD-B15 | Is the measure of the goal reached (a number or an observable fact), as the Product Owner defined it? | The measure against the evidence | WFL-015 | G |
| AUD-B16 | Did all changes to scope come through the Product Owner and the backlog? | The change record | WFL-090 | E G |

| ID | Rule |
|---|---|
| AUD-040 | An audit decision needs a verdict for each lens: **pass**, **pass with findings** (all open findings accepted or scheduled, section 9) or **fail**. All three verdicts are written in the report. |
| AUD-041 | An item that does not apply is shown as "not applicable" with its reason, and never left out (`workflow.md` WFL-058). |
| AUD-042 | The three lenses are judged on the same build id and the same run. A lens judged on another build is invalid. |
| AUD-043 | The Auditor does the business lens by using the product, not by reading the tests. A passing test is evidence for the tests lens, not for the business lens. |
| AUD-044 | The business lens does not decide scope. A missing feature is a new story for the Product Owner (WFL-112), not a finding against the build. |
| AUD-045 | The checklists apply as marked in the "Applies" column. A project MAY add items (AUD-081); it MUST NOT remove an item without a decision of the Architect. |

## 6. The method of an audit

Every audit, at every point, follows the same eight steps. The depth changes; the steps do not.

| Step | Activity | Output |
|---|---|---|
| 1. Scope and sample | State the audit point, the build id, the sources it is audited against (requirements, contracts, story cards) and, at AP1, the sample (AU01) | The scope block of the report |
| 2. Prepare | Take a clean workspace and a clean checkout of the build id. Read the evidence block, the requirements, the contracts and the registers of the scope. Read the previous report and its open findings | A list of what to check |
| 3. Run | Run the pipeline commands (AUD-030); run the smoke check and the sampled criteria on the deployed build; run the upgrade path where a release exists | Commands, build id and results |
| 4. Inventory | Compare what exists with what the documents require: stories, requirements, files, tables, endpoints, screens, tests. Mark match or gap | The inventory table |
| 5. Lenses | Go through the checklists of the three lenses that apply at this point (section 5) | Items with result and evidence |
| 6. Cross-domain | At AP2 and AP3, verify the connections and journeys across domains (section 8) | The connection and journey results |
| 7. Findings and patterns | Write the findings (section 7); group repeated causes (section 10) | Findings and pattern list |
| 8. Decide and record | Give a verdict per lens, decide pass or not (section 9), write the report, hand it to the Lead; the Product Owner and the Architect accept or reject open findings | The signed report |

| ID | Rule |
|---|---|
| AUD-050 | The sample at AP1 is chosen by the Auditor after the stories are merged, from the list of merged stories: all stories with a data or contract change, all defect stories, all stories that change the pipeline or the infrastructure, and one in AU02 of the others, and at least AU05 per epic. The selection is not announced before the merge. |
| AUD-051 | The pipeline checks every story's evidence block automatically or, at the MVP, the Lead checks it by hand (`ci-cd.md` CIC-249). The Auditor's in-depth audit of a sampled story re-runs its criteria on the deployed build and reads the code and tests of the story. |
| AUD-052 | The workspace of an audit is separate from the workspace of the authors. The Auditor does not use a build that someone prepared for it. |
| AUD-053 | The report states what was run, what was read and what was not checked. "Not verified" is written as such, with the reason. A report does not imply more than was done (AGT-R15). |
| AUD-054 | The Auditor MAY ask the owner of a work item a question about facts (where is X, how is Y started). It does not ask the owner whether the work is correct and does not take the owner's word as evidence. |
| AUD-055 | The Auditor fixes nothing. A defect found is a finding with a fix story for the owner of the cause. A trivial correction that the Auditor could make (a typo in a document) is still a finding for the owner (AUD-R01). |
| AUD-056 | After findings are fixed, the re-audit covers the findings, everything they touched, and a full run of the pipeline. It is a new audit step with its own record, not an edit of the old one. |
| AUD-057 | Three full loops without a pass go to the Lead and the Architect (`workflow.md` WFL-064, `agents.md` AGT-023). The Auditor then adds the cause to the patterns. |
| AUD-058 | The audit is time-boxed (AU04). When the box is used up, the Auditor reports what is covered and what is not, and the Lead decides whether to extend or to accept the gap in writing. |

## 7. Findings: severity, record, lifecycle

| Severity | Meaning | Examples |
|---|---|---|
| **Critical** | The build must not proceed: the product gives a wrong business result, can lose or corrupt data, cannot build, start or deploy, breaks a contract used by others, leaks a secret, or a gate was bypassed | A migration that destroys data; a secret in Git; a red pipeline that was merged; a calculation that differs from the requirement |
| **Major** | A requirement, criterion or rule of a contract is not met, or is not proven, and there is a workaround or a limited effect | A criterion without a test; a cross-domain journey that fails; a domain border crossed; a shortcut not registered |
| **Minor** | A quality gap that does not break a requirement | Unclear text; a missing README line; a naming inconsistency; a small duplication |
| **Observation** | A note for learning, without a rule behind it | A suggestion for a clearer template; a risk to watch |

Finding record (12.4): ID `F-<scope>-nn`, severity, lens (T, Q or B), the audit point and build id, the checklist item, the rule or requirement broken, the evidence, the owner of the cause, the fix story, the status.

| State | Meaning |
|---|---|
| Open | Found, with evidence |
| Assigned | A fix story with an owner exists |
| Fixed | The owner says it is fixed (the owner's claim) |
| Verified | The Auditor re-checked it on a build and saw it fixed |
| Closed | Verified, and recorded in the report |
| Accepted | Not fixed on purpose; accepted in writing by the Product Owner and the Architect, with a reason and a date (WFL-260) |
| Withdrawn | The Auditor found that the finding was wrong; the reason is recorded |

| ID | Rule |
|---|---|
| AUD-060 | Every finding has evidence that anyone can check again, a reference to the rule or requirement it breaks, and an owner of the cause. A finding without these is an observation. |
| AUD-061 | A critical finding stops the line: no further story merges or deploys until it is fixed and verified, or the Product Owner and the Architect decide otherwise in writing. |
| AUD-062 | A major finding must be fixed and verified, or accepted, before the gate of the audit point passes. A minor finding is fixed before the next gate, or accepted with a date. An observation needs no action. |
| AUD-063 | Only the Auditor sets a finding to Verified, Closed or Withdrawn. Only the Product Owner and the Architect together set a finding to Accepted. The owner of the cause sets it to Fixed. |
| AUD-064 | The severity is set by the Auditor from the table. The Product Owner may argue it with evidence; a change of severity is recorded with the reason (WFL-E31). |
| AUD-065 | Findings are kept in the register `ai-docs/audit/findings.md` and in the report of the audit that found them. A closed finding is never deleted; a reopened one gets a new entry that refers to the old one. |
| AUD-066 | A finding found by someone else (the Reviewer, a tester, a user) enters the register as well, with its source, so that the register is the one list of known gaps. |
| AUD-067 | A finding about a document is a finding against the owner of that document (`workflow.md` 12.6); the owner changes it. |
| AUD-068 | Findings are counted by cause, not by person. The count of findings is not a measure that anyone is rewarded for (WFL-251). |

## 8. Cross-domain verification

The question of goal G-03 is: "can the data of every domain be reached and used by the other domains in the ways the business cases need?" (`workflow.md` WFL-113). This section fixes how the Auditor checks it. At epic gates the same checks run for the domains that exist so far; at G-03 they run for all.

| Check | How | Evidence | Items |
|---|---|---|---|
| Connection inventory | Build the list of connections from three sources: the code (imports and calls through published interfaces), the schema (any cross-domain reference) and the contract (API operations, events). Compare it with the dependency matrix of the Architect | The connection matrix; any difference is a finding | AUD-T02, AUD-T05 |
| Cross-domain journeys | For each business case that crosses domains: start from seeded data, create or change data in the first domain through its published operation, and check that it is visible and usable in the next domain through the published surface (read, event, screen), that a change through the right operation shows at the other side, and that an error in one domain is handled in the other as the README states | One end-to-end scenario per case, results, screenshots | AUD-B06, AUD-Q10 |
| Data ownership and consistency | Each table has one owner; no shared table; no foreign key across a domain border; migrations from empty and from the last release; seeds twice | The data audit record | AUD-T04, AUD-T05, AUD-T19 |
| Contract audit | The API description equals the running API; the client equals the description; the events published equal the catalogue | The contract diff report with zero differences | AUD-T03 |
| Failure behaviour | An unavailable, slow or failing dependency; duplicate or late events; a double submit; concurrent changes: each domain behaves as its README states | The failure test results | AUD-T08 |
| Language and accessibility sweep | All screens in all supported languages with the accessibility checks | The findings | AUD-B07, AUD-B08, AUD-Q11 |
| Clean-start acceptance | A new workspace, one command, seed, all journeys | The release candidate record | AUD-T20 |

| ID | Rule |
|---|---|
| AUD-110 | The Auditor does not read another domain's tables in a journey. A journey that works only through a direct database read is a finding: the published surface is missing or not used. |
| AUD-111 | A connection that exists in the code or the schema and is not in the dependency matrix is a major finding. A connection in the matrix that does not exist is a minor finding (a stale matrix). |
| AUD-112 | "Accessible between domains" means functionally reachable. It does not include access control (WFL-114). |
| AUD-113 | A journey that fails is a defect against the domain that owns the cause, found by the Debug Engineer (or the Lead) with the Auditor's evidence. The fix goes through the normal loop (WFL-112). |
| AUD-114 | The list of business cases comes from `requirements.md` and the epic list. A business case with no journey is a major finding at the epic gate in which its domains are closed. |
| AUD-115 | At G-03 every requirement and every acceptance criterion maps to a passing test and every connection is listed: the traceability matrix has no gap (WFL-115). |

## 9. Decisions and gates

The verdicts of the three lenses (AUD-040) give the decision of the audit point.

| Lens verdicts | Open findings | Decision |
|---|---|---|
| All pass | None, or observations only | **Pass** |
| All pass or pass with findings | Only minor findings, accepted or scheduled with a date; no open critical or major | **Pass with findings** |
| Any lens fails | An open critical or major finding that is not accepted | **Fail**: the story, the epic, the goal or the release does not pass |

| ID | Rule |
|---|---|
| AUD-070 | A pass is a decision, not a ceremony: if a check fails, the unit audited stays where it is (a story returns to its owner; an epic stays in Gate state, WFL-077) and the Lead creates the fix stories. |
| AUD-071 | Acceptance of an open major finding is rare, written, and carries the reason, the approvers and a date by which it is reviewed again. A critical finding is not accepted; it is fixed. |
| AUD-072 | The audit decision is one input to the gate. The gate also needs the Lead's checklist (`workflow.md` 11) and the sign-off of the Product Owner and the Architect (goal gates). The Auditor never closes an epic or a goal. |
| AUD-073 | The Product Owner approves a promotion to prod only with a release audit record of the same build id with the decision pass or pass with findings (AUD-033). |
| AUD-074 | If the Auditor and the Product Owner disagree on a finding, it stays open until the Product Owner and the Architect decide and record it (`workflow.md` WFL-E31). |
| AUD-075 | When the number of accepted minor findings at a gate exceeds AU09, the Architect reviews the trend before the gate passes: a growing pile of accepted gaps is itself a finding. |
| AUD-076 | A decision applies to one build id. A later change to the audited state invalidates the decision for the changed part and everything that depends on it. |

## 10. Patterns and learning

| ID | Rule |
|---|---|
| AUD-080 | At each epic gate the Auditor groups the findings by cause (missing criterion, unclear contract, forgotten edge case, flaky test, wrong dependency, unregistered shortcut, and so on) and counts them. A cause that occurs twice in an epic or in two epics is a pattern (`workflow.md` 7.5). |
| AUD-081 | A finding that no checklist item would have caught is turned into a new checklist item, by proposal to the Architect. The item says the question, the check and the source rule. An item that has never found or prevented anything for three epics is reviewed for removal. |
| AUD-082 | For each pattern the Auditor proposes the cheapest effective change in this order: an automated check in the pipeline, a checklist item or a Definition-of-Ready item, a clearer rule or text in a contract, a task-spec template change (`workflow.md` WFL-150). "Be more careful" is not a proposal. |
| AUD-083 | The Auditor proposes; the Architect decides; the owner of the document changes it. The Auditor does not change a rule, a gate or its own method (AGT-R11). |
| AUD-084 | The report of each epic and goal includes the process metrics that the Lead provides (`workflow.md` 7.9, WFL-250) and states what they suggest. |
| AUD-085 | The audit records `ai-docs/audit/patterns.md` list the patterns, the change made and the result at the next gate, so that the learning can be checked (WFL-151). |

## 11. Independence, access and records

| Aspect | Rule |
|---|---|
| Role | The Auditor is a separate agent instance with its own context, on the best available model (TEAM-050, TEAM-053). It is active from G-01. |
| Reads | Everything in the repository, the pipeline results, the deployed environments (read), the reports and the run records. Never secrets: it uses the smoke and test commands, not credentials. |
| Writes | Only the audit records under `ai-docs/audit/` and scratch work outside the repository. No code, test, contract, pipeline, infrastructure or other document (`agents.md` 11). |
| Input | The contracts, the requirements, the story evidence and the build. Not the author's reasoning or private notes (`agents.md` AGT-083). |

Records in `ai-docs/audit/`:

```text
ai-docs/audit/
  README.md             the method notes of this project and the register of simplifications (AUD-020)
  findings.md           the register of all findings with their state (AUD-065)
  patterns.md           the patterns, the changes proposed and the results (AUD-085)
  deferred-risks.md     the deferred-risk register (AUD-100)
  reports/
    AR-<point>-<scope>-<date>.md     for example AR-AP2-E-03-2026-11-02.md, AR-AP4-v1.0.0-2026-12-01.md
```

| ID | Rule |
|---|---|
| AUD-090 | Audit records are files in Git, written by the Auditor, reviewed by the Lead for format only. A record is never edited after it is signed; a correction is a new record that refers to the old one. |
| AUD-091 | A story audit record is part of the story's evidence block (`workflow.md` 17.3); a longer note goes to a report. |
| AUD-092 | The Auditor works only on the build id it states. If the build changes during the audit, the Auditor notes it and either restarts or limits the scope. |
| AUD-093 | The records contain no secrets and no personal data. Evidence that would show them (a log, a dump) is described, not copied (AGT-043). |
| AUD-094 | The Auditor's own method and checklist are documents like the others: this file, changed by a story, reviewed, and versioned. |
| AUD-095 | Access to deployed environments is for the smoke and journey commands; the Auditor does not log into hosts or change anything there (`agents.md` AGT-R08). |

## 12. Reports and templates

### 12.1 Story audit record (AP1)

```text
Story:           US-ee.ss  title                       Type: feature | technical | defect | spike | audit
Build id:        <commit>      Sample: yes (reason) | by evidence only
Evidence block:  complete | gaps: ...
Run:             ci/run all-local <result>; smoke <env> <result>; criteria re-run on the deployed build: <n of n>
Technical:       pass | pass with findings | fail     items checked: AUD-T..  findings: F-...
Tests:           pass | pass with findings | fail     items checked: AUD-Q..  findings: F-...
Business:        pass | pass with findings | fail     items checked: AUD-B..  findings: F-...
Decision:        Done | returns to <owner> because <finding>
Auditor / date:
```

### 12.2 Epic or goal audit report (AP2, AP3, AP6; extends `workflow.md` 17.5)

```text
Scope:           epic or goal audited; the sources it was audited against (requirements, contracts)
Audit point:     AP2 | AP3 | AP6        Date:        Build id:
Build and run:   commands run and results (build, tests, deploy, seed, smoke, upgrade path)
Inventory:       what exists compared with what the documents require (match / gap)
Technical lens:  checklist AUD-T.. with result and evidence; verdict
Tests lens:      checklist AUD-Q.. with result and evidence; verdict; traceability result
Business lens:   checklist AUD-B.. with result and evidence; verdict; the demo run on the deployed build
Cross-domain:    connections and journeys checked and results (section 8)
Findings:        id, severity, lens, area, evidence, rule or requirement, owner, state
Patterns:        repeated causes; proposals (section 10)
Metrics:         from the Lead (WFL-250) and what they suggest
Deferred risks:  the register at this date, any change, the Product Owner's acknowledgement
Not verified:    what was not checked and why
Decision:        pass | pass with findings | fail; accepted findings with approvers and dates
Signed:          Auditor; Product Owner and Architect for acceptances
```

### 12.3 Release audit record (AP4)

```text
Build id / tag:  <commit> / v<major>.<minor>.<patch>     Target: test | prod
Evidence:        green run on the previous environment (CIC-141); digests equal (CIC-001)
Technical:       AUD-T01, T03, T04, T07, T10, T11, T12, T16, T17 results
Tests:           AUD-Q01, Q02, Q12, Q14, Q15 results
Business:        AUD-B01, B06, B09, B11, B12, B14 results; release notes; known issues
Rollback:        the previous build id; rollback ready and tested (CIC-143); backup verified (CIC-135)
Approval needed: Product Owner (prod) — recorded by name and date (CIC-142)
Decision:        pass | pass with findings | fail
```

### 12.4 Finding record

```text
ID:          F-E03-07           Severity: critical | major | minor | observation     Lens: T | Q | B
Found at:    AP2 epic E-03, build <commit>, item AUD-Q01
Rule:        TST 17 / REQ-045 / BKD-SLC-004 ...
Evidence:    command and output | file and line | run id | screenshot description
Cause owner: <role>             Fix story: US-...        State: open | assigned | fixed | verified | closed | accepted | withdrawn
Accepted by: PO, Architect, date, reason, review date  (only when accepted)
```

### 12.5 Deferred-risk register (`ai-docs/audit/deferred-risks.md`)

| Risk | Source | Accepted by | Reviewed at |
|---|---|---|---|
| No authentication, no roles, no access control | WFL-120 | Product Owner, date | Each goal gate |
| Environments served over plain HTTP, reachable without access control | INF-NET-006 | Product Owner, date | Each goal gate |
| Demo data only; no real personal data in any environment | INF-DAT-015 | Product Owner, date | Each goal gate |
| No monitoring or alerting; recovery relies on health checks, the run record and incident records | INF-DEF-001, INF-DPD-009 | Product Owner, date | Each goal gate |
| One host and one database volume are single points of failure | INF-DPD-004 | Product Owner, date | Each goal gate |
| A short interruption at each deployment; no zero-downtime release | INF-DEP-004 | Product Owner, date | Release |
| MVP shortcuts in each area (the registers) | AUD-T13 | Architect | Each epic gate |

## 13. Deferred areas: what the Auditor records

| Area | State | What the Auditor does |
|---|---|---|
| Security audit | Not performed; no-touch (WFL-120, AGT-010) | Keeps the deferred-risk register; checks that no security feature was built early (AUD-T16); records a flaw it notices as a finding of the class "deferred" for the dormant phase (`agents.md` AGT-E04) |
| Observability audit | Not performed; no-touch | Checks that the reserved places exist (health endpoints, container log limits, the run record, incident records) and that none was replaced by a monitoring tool |

| ID | Rule |
|---|---|
| AUD-100 | The Auditor keeps the deferred-risk register (12.5). A new deferred risk (a new shortcut with a risk, a new environment without protection) is added when it appears, and the Product Owner acknowledges it in the next report. |
| AUD-101 | A security flaw found during an audit is recorded as a deferred item with evidence, not fixed, not graded as a defect of the audited work (unless the correctness rules of the area contract were broken, for example a secret in the repository, which is a finding of AUD-T07). |
| AUD-102 | The correctness and hygiene rules that look like security (validation, error handling, no secrets in code or Git, key-only deploy access, log limits) are audited as correctness (AUD-T07, AUD-T08) and stay in scope (`infrastructure.md` INF-DEF-002). |
| AUD-103 | When the dormant phase is opened (AP6), the Auditor lists the deferred risks, the state of the product and the proposed order of the first security and observability stories. It then leads the security and observability audits as a new part of this document (AX5). |
| AUD-104 | The documents `security.md` and `observability.md` are not created (`architecture.md` 7). A security or observability checklist is added to this document only when the phase is opened. |

## 14. Edge cases

| ID | Situation | Handling |
|---|---|---|
| AUD-E01 | The pipeline is green but the Auditor's fresh run is red. | The Auditor's run counts as evidence; the difference is a finding (a flaky test, an environment difference, a missing step) and goes to the patterns (AUD-Q08). |
| AUD-E02 | The status report says "green" and the audit finds gaps. | The audit prevails; the status is corrected and the cause becomes a pattern (`workflow.md` WFL-E39, AUD-002). |
| AUD-E03 | A story in the sample was changed after the Product Owner accepted it. | The acceptance is invalid for the changed part; the story returns to stage 13 (AUD-076). |
| AUD-E04 | The evidence block is incomplete for a story outside the sample. | A major finding against the Lead (`workflow.md` WFL-051); the story returns to the Lead; the pipeline's evidence check, if present, is reviewed (AX1). |
| AUD-E05 | The Auditor finds a defect during an audit and the owner says it is "by design". | The Auditor cites the rule or requirement. If none exists, it is an observation; the Product Owner or the Architect decides, and the decision is recorded (AUD-064). |
| AUD-E06 | The requirement itself is wrong or unclear. | A finding against `requirements.md` (the Product Owner is the owner); the code is not a finding for conforming to it; the Business Analyst helps clarify (`agents.md` AGT-E03). |
| AUD-E07 | A finding is in two areas at once. | One finding per cause; the Lead splits the fix by owner (`team.md` TEAM-E05); the finding stays open until all parts are verified. |
| AUD-E08 | The best model is not available for the Auditor. | The best available is used and recorded (`agents.md` AGT-E11); a note in the report says which model was used. |
| AUD-E09 | There is no deployed environment yet (before the first remote host). | The audit uses the local environment and states it; items that need an environment are "not applicable (no environment yet)" until G-01 closes (`infrastructure.md` INF-ENV-008). |
| AUD-E10 | The Auditor is asked to audit its own earlier change (a record, a rule). | Another instance or the Architect audits it; the Auditor does not audit itself (AUD-001). |
| AUD-E11 | A fix for a critical finding is urgent (a production issue). | The hotfix lane applies (`workflow.md` 7.8); the Auditor verifies the fix and records AP5 afterwards; nothing is skipped except planning and documentation. |
| AUD-E12 | The audit time-box is too short for a large epic. | The Auditor reports what is covered; the Lead extends the box or accepts the gap in writing (AUD-058). |
| AUD-E13 | Two audits (two epics) overlap in time on the same code. | Each audit states its build id; a finding that applies to both is recorded once and linked. |
| AUD-E14 | A finding recurs after being closed. | A new finding with a link to the old one; the pattern is raised and an automated check is proposed (AUD-082). |
| AUD-E15 | A test passes but the behaviour is wrong in use. | Tests lens: the test is weak (finding against the test, AUD-Q05, and a probe if AX3 fires); business lens: the defect is recorded against the story. |
| AUD-E16 | A criterion is demonstrated in tests but cannot be demonstrated on the deployed build. | A major finding: the environments differ or the criterion is not delivered (AUD-T11, AUD-B02). |
| AUD-E17 | A requirement exists only as a wish and was never written as a requirement. | An observation for the Product Owner: it is not in scope until it is (WFL-090). |
| AUD-E18 | The product is correct but unusable for the persona. | A finding of the business lens (AUD-B03, AUD-B08): major when a MUST requirement cannot be done by the persona; minor otherwise. |
| AUD-E19 | A shortcut is registered but its trigger has fired. | A major finding: the shortcut must end or the Architect must extend it in writing (INF-MVP-001, CIC-250). |
| AUD-E20 | A deployment is rolled back during the audit. | The audit pauses; the rollback is audited as an event (AUD-T10); the audit restarts on the restored build id. |
| AUD-E21 | The Product Owner is unavailable to accept findings. | The gate waits; work that does not depend on it continues (`team.md` TEAM-E03). |
| AUD-E22 | The Auditor cannot reach an environment (a host is down). | The affected items are "not verified" with the reason; the decision cannot be a pass for them (AUD-053). |
| AUD-E23 | A document the audit depends on is missing (for example `requirements.md` before G-02). | The audit uses the story cards and records the gap as a finding against the owner. |
| AUD-E24 | An audit finds that the audit method itself missed something. | The method is changed through the pattern process (AUD-081), not silently. |
| AUD-E25 | Many small findings arrive at once after a long gap between audits. | The sample interval is shortened (AU02) and the cause goes to the patterns; late audits are an anti-pattern (AUD-035). |

## 15. Restrictions

| ID | Restriction |
|---|---|
| AUD-R01 | The Auditor changes nothing except audit records: no code, test, contract, pipeline, infrastructure or document of another owner. |
| AUD-R02 | The Auditor does not audit its own work, and nobody audits work that they built, reviewed or accepted. |
| AUD-R03 | No audit decision without a run on a clean checkout of the stated build id. |
| AUD-R04 | No audit decision with fewer than the three lenses. |
| AUD-R05 | No finding without evidence and a rule or requirement reference; no verdict without checked items. |
| AUD-R06 | No acceptance of a finding by anyone but the Product Owner and the Architect together, in writing. |
| AUD-R07 | No acceptance of a critical finding. |
| AUD-R08 | No edit of a signed record; corrections are new records. |
| AUD-R09 | No secrets, credentials or personal data in an audit record. |
| AUD-R10 | No security or observability audit and no security or observability checklist before the dormant phase is opened. |
| AUD-R11 | No audit of a build that changed during the audit without a restart or an explicit limit of scope. |
| AUD-R12 | No use of the author's explanation as evidence. |
| AUD-R13 | No login to a host, no change to an environment, no use of credentials by the Auditor. |
| AUD-R14 | No removal of a checklist item or an audit point without a decision of the Architect. |
| AUD-R15 | No closing of an epic, a goal or a story by the Auditor. |
| AUD-R16 | No report that claims more than was done: unverified items are stated as such. |

## 16. Parameters

| ID | Parameter | Default | Note |
|---|---|---|---|
| AU01 | Stories audited in depth at AP1 | All with a data or contract change; all defects; all pipeline and infrastructure stories; the sample of AU02 | AUD-050 |
| AU02 | Share of the other stories in the sample | One in four, at random | Shorten after a late audit (AUD-E25) |
| AU03 | Fix timing by severity | Critical: stop the line; major: before the gate; minor: before the next gate | AUD-061, AUD-062 |
| AU04 | Time-box of an audit | A story: one working session; an epic: two sessions; a goal or release: set by the Lead | AUD-058 |
| AU05 | Minimum stories audited in depth per epic | 1 | AUD-050 |
| AU06 | Maximum audit loops before escalation | 3 | AUD-057 |
| AU07 | Retention of audit records | Permanent, in Git | AUD-090 |
| AU08 | Age of the evidence | The last full run must be on the merged state of the audited build | AUD-Q02 |
| AU09 | Accepted minor findings at one gate before the Architect reviews the trend | 10 | AUD-075 |
| AU10 | Release audit for test | The items of 12.3 marked for the technical and tests lens; the business items when the Product Owner uses test for acceptance | AP4 |

## 17. Sources and limits

| Source | Used for |
|---|---|
| `workflow.md` 5.2, 7.5, 8, 10.3, 11, 12.6, 17.3, 17.5 | Stage 16, patterns, the Definition of Done, the verification epic, gates, records |
| `team.md`, `agents.md`, `architecture.md` | The Auditor's role, independence, limits, the deferred areas |
| `tests.md`, `ci-cd.md`, `infrastructure.md`, `db.md`, `backend.md`, `frontend.md`, `api.md`, `ux-ui.md`, `i18n.md` | The rules that the checklists check |
| The uploaded audit reports and the final review of the example projects (build verification table, file inventory against the specification, issues by severity with root cause and fix, sign-off of the definition of done) | The shape of the report: a run table, an inventory, findings with a fix and a re-check, and a definition-of-done sign-off |

Limits of this version. The checklists were derived from the project documents and from the uploaded example audits. They were not tested on a real audit; the first audits (G-01, and the first epic) will show which items find nothing and which gaps are not covered (AUD-081). The three-lens structure and the thresholds are a design choice of this project, not a standard. No quality-model standard or audit standard was read in full for this version.
