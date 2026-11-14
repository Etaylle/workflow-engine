# Software elements: from screen element to table, and what the AI writes

| | |
|---|---|
| Document | `ai-docs/software-elements.md` |
| Version / status | 0.3, draft for review (13 November 2026): Claude only, session-only API key, all-projects page, GitHub and Render of the project starter (ADR-032 to ADR-039); 0.2: GitHub push, story types, error types, evidence, audit report, metrics, lessons, language, edit conflicts, dialogs and states |
| Purpose | One map for every element of the twelve MVP screens (in the order 1 Wish, 2 Questions, 3 Requirements, 4 Workflow, 5 Edit a task, 6 AI team, 7 Development workflow, 8 Ready to run, 9 Workflow in process, 10 Run details, 11 Task details, 12 Human action required, 13 Dialogs and states): the Svelte component that shows it, the API operation, the backend class, the database table, and who writes the data (the client, the code, or which AI agent with which structured output). |
| Owner | Architect (structure), with the Lead Developer |
| Screens | `ui-mockup/images/NN-*.png` (one per page, numbered as in this document), `ui-mockup/index.html` |
| Skeleton | In `ai-docs/skeleton/` until Epic 0 places it in the monorepo; paths relative to that folder: `backend/<domain>/migrations/V1__<domain>_schema.sql`, `backend/<domain>/src/main/java/engine/<domain>/domain/*.java`, `backend/<domain>/src/main/java/engine/<domain>/application/ai/*.java`, `backend/shared/…`, `frontend/src/lib/<domain>/models/<domain>.ts`, `backend/settings/seeds/default_team_profile.sql` |
| Checked | All six migrations ran on PostgreSQL 16 (42 tables); the seed ran twice with the same result; the Java skeleton compiles with `javac` (Java 21, no framework); the TypeScript models pass `tsc --strict` |

The skeleton is the starting point of Epic 0, not finished code. Contract first still holds (ADR-007): `api/openapi.yaml` is written from this map in Epic 0, and the frontend models are then generated from it. Keep the names in this document, the tables, the Java records and the TypeScript models equal; change them together.

## 1. The chain

Every element on a screen follows the same chain. The AI never writes to the database directly: it returns a structured output (a Java record, validated as JSON schema), or it calls a tool, and the code of the domain writes the rows.

```text
Screen element ─► Svelte component ─► API operation ─► use case (application) ─► domain record ─► table
                                                                 ▲
                         AI agent ─► structured output record ───┘   (validated, one retry, agent_calls row)
                         AI agent ─► @Tool method (WorkflowTools) ─┘   (same validation as the REST API)
```

| Layer | Where | Rule |
|---|---|---|
| Table | `backend/<domain>/migrations/` | One schema per domain, plural `snake_case` tables, `id` made by the application, `created_at`/`updated_at`, status as `text` with `CHECK` (db.md). No foreign key or join across schemas: other domains are referenced by id or key only (ADR-004). |
| Domain record | `engine.<domain>.domain` | One plain Java record per table, no framework annotation (backend.md BKD-DPN-011). Enums carry the database text (`dbValue()`). Enums used by two domains live in `engine.shared`. |
| AI output | `engine.<domain>.application.ai` | One record per agent result; the Javadoc names the target columns and the validation. |
| Frontend model | `frontend/src/lib/<domain>/models/` | camelCase, times as ISO strings, money as decimal string. Never contains a key, a file content or other bytes. |

## 2. Domains

| Domain | Schema | Tables | Main screens |
|---|---|---|---|
| `projects` | `projects` | projects, project_attachments, project_briefs, github_connections (no table for keys or tokens: session memory only) | 0 All projects, 1 Wish and AI, 8 Ready to run (GitHub, Render) |
| `questions` | `questions` | questions, answers, research_sources | 2 Questions and technology |
| `documents` | `documents` | epics, requirements, generated_documents | 3 Requirements |
| `workflows` | `workflows` | workflows, lanes, nodes, edges, acceptance_criteria, scenarios, node_requirements, contracts | 4 Workflow, 5 Edit a task, 8 Ready to run |
| `settings` | `settings` | team_profiles, team_members, dev_workflow_steps, dev_workflow_step_members, workflow_rules, lessons, provider_models | 6 AI team, 7 Development workflow |
| `execution` | `execution` | runs, task_runs, step_runs, ci_runs, console_lines, run_errors, human_questions, decisions, checkpoints, criterion_tests, verdicts, member_reports, audit_reports, findings, run_metrics, agent_calls, jobs | 9 Workflow in process, 10 Run details, 11 Task details, 12 Human action required |

## 3. Screen by screen

Written by: **Client** = typed or clicked by the client; **Code** = the engine without AI; an agent name = the AI agent and its output record.

### 3.0 Page 0: All projects (start page, no login)

| Screen element | Svelte component | API operation | Backend class | Table | Written by |
|---|---|---|---|---|---|
| List of all projects: name, status, starter's GitHub account, last change | `ProjectList` | `GET /api/projects` | `Project`, `GithubConnection` | `projects.projects`, `projects.github_connections` | Code |
| Open, Continue | `ProjectList` | `GET /api/projects/{id}`; continuing a run needs a key in this session (`ApiKeyField` dialog) | `Project`, `SessionSecrets` | `projects.projects` | Client; anyone may open any project (ADR-034) |
| New project | `PageFooter` | navigates to page 1 | – | – | Client |

### 3.1 Page 1: Wish and AI

| Screen element | Svelte component | API operation | Backend class | Table | Written by |
|---|---|---|---|---|---|
| Language switch in the header (every page) | `LanguageSwitch` | none (browser setting) | – | – | Client; texts from the translation resource (English, German) |
| Language of the documents | `WishForm` | `PATCH /api/projects/{id}` | `Project.documentLanguage` | `projects.projects` | Client |
| Project name, wish text | `WishForm` | `POST /api/projects`, `PATCH /api/projects/{id}` | `Project` | `projects.projects` (name, wish) | Client |
| Links | `LinkList` | `POST /api/projects/{id}/attachments` (kind link) | `ProjectAttachment` | `projects.project_attachments` | Client |
| Files | `FileDropZone`, `AttachmentList` | same, multipart; `DELETE …/attachments/{aid}` | `ProjectAttachment` | `projects.project_attachments` (content) | Client |
| Agent (opencode, Claude Code) | `AiSettingsCard` | `PATCH /api/projects/{id}` | `Project.agentHarness` | `projects.projects` | Client |
| Model (Claude) | `AiSettingsCard` | same; models from `GET /api/models` | `Project.provider` (always `anthropic`), `.model` | `projects.projects`; `settings.provider_models` | Client |
| API key for this session, Test | `ApiKeyField` | `PUT /api/session/api-key` (tests the key with one call; `DELETE` forgets it); `GET /api/session` returns only `hasKey` | `SessionSecrets` (in memory, per browser session; no domain record) | none: never stored | Client; never returned, never logged; asked again in a new session, after a restart and when a run is continued without a key |
| GitHub sign-in at the start (optional) | `GithubCard` | `POST /api/projects/{id}/github/device` (starts the device flow), `GET …/github` (status), `DELETE …/github` | `GithubConnection`; token in `SessionSecrets` | `projects.github_connections` (account_login, repository_name, visibility public; no token column) | Client; without it the delivery is the ZIP only |
| Send to the AI | `PageFooter` | `POST /api/projects/{id}/submit` → event WishSubmitted | `Project.status` | `projects.projects` | Code |
| (not shown) project brief | – | – | `ProjectBrief` | `projects.project_briefs` | Prompt agent → `ProjectBriefOutput` |

### 3.2 Page 2: Questions and technology

| Screen element | Svelte component | API operation | Backend class | Table | Written by |
|---|---|---|---|---|---|
| Question cards (one choice, several, text, feature) | `QuestionList`, `QuestionCard` | `GET /api/projects/{id}/questions` | `Question` | `questions.questions` | Questions agent → `QuestionSetOutput` |
| Answer | `QuestionCard` | `PUT /api/questions/{qid}/answer` | `Answer` | `questions.answers` (decided_by client) | Client |
| "Let the AI decide" | `QuestionCard` | same with `decidedBy: ai` | `Answer` | `questions.answers` (value = ai_default) | Code, from `questions.ai_default` |
| Progress "14 of 22" | `AnswerProgress` | computed from the list | – | – | Code |
| Backend, frontend, database | `StackPicker` | `PUT /api/projects/{id}/stack` | `Project.backendStack`, `.frontendStack` | `projects.projects` | Client; templates from the manifest |
| Sources (details) | `SourceList` | `GET /api/projects/{id}/research-sources` | `ResearchSource` | `questions.research_sources` | Questions agent |
| Create requirements | `PageFooter` | `POST /api/projects/{id}/answers/complete` → AnswersSaved | – | – | Code |

### 3.3 Page 3: Requirements

| Screen element | Svelte component | API operation | Backend class | Table | Written by |
|---|---|---|---|---|---|
| Epic group header | `EpicGroup` | `GET /api/projects/{id}/requirements` (with epics) | `Epic` | `documents.epics` | Requirements agent → `RequirementSetOutput` |
| Requirement row (ID, text, priority, from) | `RequirementRow` | same; `PATCH /api/requirements/{rid}` | `Requirement` | `documents.requirements` | Requirements agent; edited by Client |
| Add requirement | `RequirementEditDialog` | `POST /api/projects/{id}/requirements` | `Requirement` | `documents.requirements` | Client |
| Summary, out of scope | `RequirementSummary` | same | – | – | Code |
| Download requirements / project plan | `DocumentDownloads` | `GET /api/projects/{id}/documents/{kind}?format=docx` | `GeneratedDocument` | `documents.generated_documents` | Code (renderer with fixed outline from the agent's records) |

### 3.4 Pages 4 and 5: Workflow and Edit a task

| Screen element | Svelte component | API operation | Backend class | Table | Written by |
|---|---|---|---|---|---|
| Canvas (Svelte Flow) | `BpmnCanvas` | `GET /api/projects/{id}/workflow` (lanes, nodes, edges) | `Workflow` | `workflows.workflows` | Workflow agent (tools) |
| Lane band and label | `LaneNode` (group node) | `POST /api/workflows/{wid}/lanes` | `Lane` | `workflows.lanes` | Workflow agent `addLane`; Client |
| Start and end circle | `EventNode` | `POST /api/workflows/{wid}/nodes` | `Node` (kind start_event, end_event) | `workflows.nodes` | Workflow agent `addEvent` |
| Task box (title, requirement count) | `TaskNode` | same; `PATCH /api/nodes/{nid}` (position, text) | `Node` (kind task) | `workflows.nodes` | Workflow agent `addTask`; Client |
| Manual task (dashed) | `TaskNode` (taskType manual) | same | `Node.taskType` | `workflows.nodes` | Workflow agent; Client |
| Arrow | `FlowEdge` | `POST /api/workflows/{wid}/edges`, `DELETE /api/edges/{eid}` | `Edge` | `workflows.edges` | Workflow agent `connect`; Client |
| + before / + after | `TaskNode` handles | `POST /api/nodes/{nid}/insert-before`, `…/insert-after` | `Node`, `Edge` | `workflows.nodes`, `workflows.edges` | Client (code rewires edges) |
| Valid badge | `ValidationBadge` | `POST /api/workflows/{wid}/validate` | – | `workflows.workflows.status` | Code (`WorkflowTools.validate`) |
| Story type (feature, technical, defect, spike, audit) | `TaskEditPanel` | `PATCH /api/nodes/{nid}` | `Node.storyType` | `workflows.nodes` | Workflow agent proposes; Client |
| Test scenarios (Given-When-Then), Write again | `ScenarioList` | `GET /api/nodes/{nid}/scenarios`, `POST …/scenarios/regenerate` | `Scenario` | `workflows.scenarios` | Workflow agent → `ScenarioSetOutput` |
| Delete task (with confirmation) | `ConfirmDialog` | `DELETE /api/nodes/{nid}` | `Node`, `Edge` | `workflows.nodes`, `workflows.edges` | Client |
| Task name, what happens, lane, type | `TaskEditPanel` | `PATCH /api/nodes/{nid}` | `Node` | `workflows.nodes` | Client |
| Done when (criteria) | `CriteriaEditor` | `PUT /api/nodes/{nid}/criteria` | `AcceptanceCriterion` | `workflows.acceptance_criteria` | Workflow agent; Client |
| Linked by the AI (epic, story, requirement IDs) | `AiLinks` | part of the node | `Node.epicKey`, `.storyKey`; `NodeRequirement` | `workflows.nodes`, `workflows.node_requirements` | Workflow agent |

### 3.5 Page 8: Ready to run

| Screen element | Svelte component | API operation | Backend class | Table | Written by |
|---|---|---|---|---|---|
| Summary rows | `ContractSummary` | `GET /api/projects/{id}/summary` | – | read from several domains through their APIs | Code |
| AI team, development workflow (Edit) | `ContractSummary` → links to pages 6 and 7 | `GET /api/team-profiles/default` | `TeamProfile` | `settings.team_profiles` | Client chooses |
| GitHub: account, repository name (public), push after every task; Deploy to Render link | `GithubCard`, `RenderLink` | `PATCH /api/projects/{id}/github`; the link is `https://dashboard.render.com/blueprint/new?repo=<repository_url>` (no API call) | `GithubConnection` | `projects.github_connections` (repository_url, repository_created_at) | Client; the repository is created on the first push by code |
| Cost limit | `LimitsCard` | part of the confirm request | `Contract.costLimitAmount` | `workflows.contracts` | Client |
| Confirm checkbox + Run | `ConfirmBar` | `POST /api/projects/{id}/contracts` then `POST /api/projects/{id}/runs` | `Contract`, `Run` | `workflows.contracts` (snapshot), `execution.runs` | Code; the snapshot includes a copy of the team and the development workflow |

### 3.6 Pages 6 and 7: AI team and development workflow (editable)

Shown after the workflow (page 4) and before Ready to run (page 8). Also opened from Run details and from the task details during a run; there they show the snapshot of the running contract, and changes apply to the next run.


| Screen element | Svelte component | API operation | Backend class | Table | Written by |
|---|---|---|---|---|---|
| Team size (Starter, Standard, Full, Custom) | `TeamPresetPicker` | `PUT /api/team-profiles/{pid}/preset` | `TeamProfile.preset` | `settings.team_profiles`, `settings.team_members.active` | Client |
| Member row (name, does, model, in the team) | `TeamTable`, `TeamMemberRow` | `GET /api/team-profiles/{pid}/members`, `PATCH /api/team-members/{mid}` | `TeamMember` | `settings.team_members` | Seed from team.md; Client |
| Edit member (instructions, may write) | `MemberEditPanel` | same; `POST …/members` | `TeamMember.instructions`, `.writePaths` | `settings.team_members` | Client |
| Reset to initial (with confirmation) | `TeamTable` toolbar, `ConfirmDialog` | `POST /api/team-profiles/{pid}/reset?part=team` or `?part=steps` | – | the settings tables of the profile | Code (runs the seed for the profile) |
| Step row (name, members, runs when) | `DevStepList`, `DevStepRow` | `GET /api/team-profiles/{pid}/steps` | `DevWorkflowStep`, `DevWorkflowStepMember` | `settings.dev_workflow_steps`, `settings.dev_workflow_step_members` | Seed from workflow.md; Client |
| Drag to reorder | `DevStepList` | `PUT /api/team-profiles/{pid}/steps/order` | `DevWorkflowStep.seq` | `settings.dev_workflow_steps` | Client; first and last stay fixed |
| Edit step (members, runs, done when, time) | `DevStepEditPanel` | `PATCH /api/dev-workflow-steps/{sid}` | `DevWorkflowStep` | `settings.dev_workflow_steps` | Client |
| Runs for story types | `DevStepEditPanel` | `PATCH /api/dev-workflow-steps/{sid}` | `DevWorkflowStep.storyTypes` | `settings.dev_workflow_steps` | Client |
| Rules: fix loops, one more try with the best model, automatic recovery by error type, CI stages | `WorkflowRulesCard` | `PUT /api/team-profiles/{pid}/rules` | `WorkflowRules` | `settings.workflow_rules` | Client |
| Lessons from earlier runs (Apply, Dismiss) | `LessonList` | `GET /api/team-profiles/{pid}/lessons`, `PATCH /api/lessons/{lid}` | `Lesson` | `settings.lessons` | Supervisor → `LessonProposalOutput`; Client decides |

### 3.7 Pages 9 to 12: the run

| Screen element | Svelte component | API operation | Backend class | Table | Written by |
|---|---|---|---|---|---|
| Time, tasks, cost, restarts | `RunStatusBar`, `StatCard` | `GET /api/runs/{rid}`; live `GET /api/runs/{rid}/events` (SSE) | `Run` | `execution.runs` | Code |
| Canvas with states | `BpmnCanvas` (read only) | `GET /api/runs/{rid}/tasks` | `TaskRun` | `execution.task_runs` (status, node_id) | Code |
| GitHub status, Open on GitHub | `GithubStatus` | `GET /api/runs/{rid}/checkpoints` | `Checkpoint` | `execution.checkpoints` (push_status, commit_url) | Code (step Merge and push) |
| Stop (with confirmation), Download ZIP | `RunActions`, `ConfirmDialog` | `POST /api/runs/{rid}/stop`, `GET /api/runs/{rid}/export.zip` | `Run`, `Checkpoint` | `execution.runs`, `execution.checkpoints` | Client; Code |
| Latest activity, console | `ActivityFeed`, `ConsoleView` | `GET /api/runs/{rid}/console?taskRunId=&after=` | `ConsoleLine` | `execution.console_lines` | Code and every agent (reports) |
| Epics and stories table | `StoryTable` | `GET /api/runs/{rid}/tasks` | `TaskRun` | `execution.task_runs` | Code |
| Agent team list | `AgentTeamList` | `GET /api/runs/{rid}/team` (from the contract snapshot) | `TeamMember` (snapshot) | `workflows.contracts.snapshot` | Code |
| Decisions | `DecisionList` | `GET /api/runs/{rid}/decisions` | `Decision` | `execution.decisions` | Product Owner → `PoAnswerOutput`; supervisor → `TaskSummaryOutput` |
| Business task card (story, criteria status) | `BusinessTaskCard` | `GET /api/task-runs/{tid}` | `TaskRun` + node data from workflows API | `execution.task_runs` | Code |
| Development workflow steps | `DevStepTimeline`, `DevStepItem` | `GET /api/task-runs/{tid}/steps` | `StepRun` | `execution.step_runs` | Supervisor → `TaskPlanOutput`; members → `AgentReportOutput` |
| CI strip (build → smoke) | `CiStrip` | `GET /api/task-runs/{tid}/ci-runs` | `CiRun` | `execution.ci_runs` (stages) | Code (`ci/run all-local`) |
| Errors with type (temporary, broken environment, flaky test, real defect, requirement conflict) | `ErrorList` | `GET /api/task-runs/{tid}/errors` | `RunError` | `execution.run_errors` (error_class, attempts, auto_action) | Code; supervisor → `RecoveryOutput` |
| Evidence: criterion → test, level, result | `EvidenceTable` | `GET /api/task-runs/{tid}/criterion-tests` | `CriterionTest` | `execution.criterion_tests` | Tester report; CI results |
| Team verdicts | `VerdictList` | `GET /api/task-runs/{tid}/verdicts` | `MemberVerdict` | `execution.verdicts` | Each member at step Team verification |
| Task spec, member reports | `ReportViewer` | `GET /api/step-runs/{sid}/reports` | `MemberReport` | `execution.member_reports` | Lead (task spec); each member → `AgentReportOutput` |
| Story type column | `StoryTable` | part of the tasks | `TaskRun.storyType` | `execution.task_runs` | Code (from the contract) |
| Audit report: three lenses, findings by severity, accepted risks, not verified | `AuditReportCard` | `GET /api/runs/{rid}/audit-report`, `GET …/audit-report.pdf` | `AuditReport`, `Finding` | `execution.audit_reports`, `execution.findings` | Auditor → `AuditReportOutput` |
| Metrics: loops per task, first try, time per task, waiting for you | `RunMetricsCard` | `GET /api/runs/{rid}/metrics` | `RunMetrics` | `execution.run_metrics` | Code |
| Human action required: question; expected, what happened, requirement, what was tried, cause; options; own answer | `HumanQuestionCard`, `AnswerOptions` | `GET /api/task-runs/{tid}/human-question`, `POST /api/human-questions/{hid}/answer` | `HumanQuestion`, `AnswerOption` | `execution.human_questions` | Supervisor → `RecoveryOutput.question`; answer by Client |
| Task summary | `TaskSummary` | part of the task run | `TaskRun.summary` | `execution.task_runs` | Supervisor → `TaskSummaryOutput` |

### 3.8 Page 13: Dialogs and states

| Screen element | Svelte component | Where it is used | Backed by |
|---|---|---|---|
| Confirm dialogs: stop the run, delete a task, reset to initial | `ConfirmDialog` (shared) | pages 5, 6, 7, 9 | The normal API call after confirmation |
| Empty states (no project, no decisions, ...) | `EmptyState` (shared) | every list | An empty collection from the API |
| Loading states (questions, run starting) | `LoadingState` (shared) | pages 2, 3, 4, 9 | A job in `execution.jobs` with status pending or processing |
| Error state (AI call failed, Try again) | `ErrorState` (shared) | every page that calls an agent | `execution.agent_calls.status` invalid_output or error; problem-detail error from the API |
| Edit conflict (changed in another tab: Reload, Show my change) | `ConflictBanner` (shared) | pages 4 and 5 | `version` column of `workflows.workflows` and `workflows.nodes`; the API answers 409 or 412 |

The app never shows the file names of the engine's own setup; buttons say "Reset to initial" and texts say "recommended".

## 4. What each AI agent writes

| Agent | Started by (event) | Input | Output | Writes (through the code) | Validation |
|---|---|---|---|---|---|
| Prompt agent | WishSubmitted | wish, attachments (as data) | `ProjectBriefOutput` | `projects.project_briefs` | goal set; 1 to 10 actors |
| Questions agent | WishAnalyzed | brief | `QuestionSetOutput` | `questions.questions`, `questions.research_sources` | 10 to 25 requirement and at most 5 feature questions; options for choice types |
| Requirements agent | AnswersSaved | brief, answers, stack | `RequirementSetOutput` | `documents.epics`, `documents.requirements` | unique keys; every requirement in an existing epic |
| Workflow agent | RequirementsReady | requirements, epics | `WorkflowTools` calls | `workflows.lanes`, `nodes`, `edges`, `acceptance_criteria`, `node_requirements` | `validate()` empty: one start, one end, all reachable, every task with an epic and a requirement |
| Development supervisor (plan) | task start | contract snapshot, task, development workflow, last results | `TaskPlanOutput` | `execution.step_runs` | one entry per step of the profile; members exist and are active |
| Team members (in the sandbox) | each step | task spec and context packet | `AgentReportOutput` | `execution.step_runs.result`, `execution.console_lines` | role is a member of the step; write paths checked by the guard and the GuardrailChecker |
| Product Owner (from the contract) | question of a member | the question, contract snapshot | `PoAnswerOutput` | `execution.decisions` when not from the contract | answer not empty; decision present when `fromContract` is false |
| Development supervisor (recovery) | AgentFailed or CI failed | error, logs, loop count | `RecoveryOutput` | `execution.run_errors`, `execution.human_questions`, `task_runs.fix_loops`, `task_runs.model_raised`, `runs.status` | error class set; temporary errors retried by code first; loops ≤ `max_fix_loops`, then one try on the best model, then ask; question in the expected/actual/requirement/tried/cause shape with 2 to 4 options |
| Workflow agent (scenarios) | after a task is drawn or edited | task, criteria | `ScenarioSetOutput` | `workflows.scenarios` | at least one Given-When-Then per criterion |
| Auditor | step Review and audit; end of the run | changed files, evidence, contracts (not the authors' reasoning) | `AuditReportOutput` | `execution.audit_reports`, `execution.findings` | severity and lens on every finding; not-verified text present |
| Development supervisor (lessons) | end of the run | errors and findings of this and earlier runs | `LessonProposalOutput` | `settings.lessons` | only causes seen at least twice |
| Development supervisor (sum up) | after Merge | step results, decisions | `TaskSummaryOutput` | `execution.task_runs.summary`, `execution.decisions` | summary not empty |

Every call writes one `execution.agent_calls` row (agent, model, tokens, time, status), never the prompt or the key (QR-SEC-01).

## 5. How the editable team and workflow reach a run

1. The default profile `Standard` is seeded from `team.md` and `workflow.md` (`ai-docs/skeleton/backend/settings/seeds/default_team_profile.sql`: 14 members, 16 steps).
2. The client edits members, steps and rules on pages 6 and 7 (the `settings` domain).
3. On confirmation (page 8) the code copies the active members, the steps and the rules into `workflows.contracts.snapshot`. A running project never changes when the profile is edited later.
4. At the start of each task the development supervisor returns a `TaskPlanOutput` with one entry per step of the snapshot; the code creates the `execution.step_runs` rows from it, in `seq` order.
5. The orchestrator runs the steps in order, skipping the steps whose `story_types` do not contain the task's story type. "Continuous integration" runs after every change; "Fix and verify" only on failure, at most `max_fix_loops` times; then the supervisor asks the client (`human_questions`), and the run waits (`runs.status = waiting_for_human`).

After step 15 "Merge and push" the code commits the checkpoint and, when a GitHub connection exists and `push_after_each_task` is set, pushes it to the branch; `execution.checkpoints.push_status` records the result. A failed push is retried once and never fails the task; the ZIP stays the fallback.

## 6. Status values

| Table | Column | Values |
|---|---|---|
| `projects.projects` | status | draft, questions_ready, answered, requirements_ready, workflow_drafted, confirmed, executing, done, stopped, failed |
| `workflows.workflows` | status | draft, valid, invalid, confirmed |
| `execution.runs` | status | queued, preparing, executing, paused, waiting_for_human, resuming, done, stopped, failed |
| `execution.task_runs` | status | pending, running, fixing, waiting_for_human, done, failed, skipped, manual |
| `execution.step_runs` | status | waiting, running, done, not_needed, failed, skipped |
| `execution.human_questions` | status | open, answered, cancelled |
| `execution.run_errors` | error_class | temporary, broken_environment, flaky_test, real_defect, requirement_conflict |
| `execution.criterion_tests` | result | waiting, running, passed, failed (levels: unit, component, integration, contract, frontend_integration, end_to_end, smoke) |
| `settings.lessons` | status | proposed, applied, dismissed |

The allowed transitions are rules of the domain, tested in code; the `CHECK` only guards the vocabulary (db.md DBS-VOC-004).

## 7. Open points

- The API paths above are a proposal for `api/openapi.yaml`; the Architect fixes them in Epic 0.
- `provider_models.model_id` holds placeholders; set the exact model ids when the first provider is wired.
- Asking the client during a run and the 16 steps per task are recorded as ADR-038 and ADR-039; measure a reference run against the 4-hour target.
- The BPMN model (`README.md` 5, files in `bpmn/`) is the reference for run states and transitions; `RunStatus` and `TaskRunStatus` must keep its end states (Result sent, Partial result sent, Key rejected; Task done, Run failed).
- `GITHUB_OAUTH_CLIENT_ID` must be set for the GitHub device flow; without it the GitHub card is hidden.
- `requirements.md` 0.3 does not yet list the features of version 0.2 of this map (GitHub push, story types, error types, evidence, audit report, metrics, lessons, language, edit conflicts, dialogs). Add them as requirements before their stories are Ready.
