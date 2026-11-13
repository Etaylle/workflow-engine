---
name: bpmn-model
description: "The BPMN 2.0 model of the workflow engine itself (level 1: run a client project; level 2: build one task with the 16 development steps): files, elements, end states and how they map to screens, status values and the execution domain. Use when implementing or reviewing run, task or step states and transitions, the orchestrator, stop, resume, errors, asking the client, GitHub push, ZIP export or the final report, or when changing the engine's flow."
---

# The BPMN model: contract for states and transitions

Files: `ai-docs/bpmn/workflow-engine-lifecycle.bpmn` (+ `.png`) and `ai-docs/bpmn/build-one-task.bpmn` (+ `.png`). Element tables, modelling rules and sources: `ai-docs/README.md` section 8. Decisions behind them: ADR-032 to ADR-039 (`architecture` skill). The model is descriptive (`isExecutable="false"`): the engine is Spring Boot code, not a BPMN engine; the code must behave as the model says.

## How to use it

1. Look at the PNG of the level you work on (`Read` the image), then the element table in `ai-docs/README.md` 8.
2. For exact names, ids, gateways and flows, search the XML: `grep -n 'name="Ask the client"' ai-docs/bpmn/build-one-task.bpmn`.
3. Map states to the status columns of `ai-docs/software-elements.md` 6 and keep every end state reachable (7). Allowed transitions are domain rules tested in code (DBS-VOC-004); write one test per BPMN path that ends in an end event.
4. A change to the flow is an architecture change: stop and ask the Architect (AGT-040); the model, ADRs and screens change together.

## Level 1: Run a client project (`workflow-engine-lifecycle`)

Pools: Client (anyone, no login); Workflow engine with lanes Engine (code), Planning agents (AI), Development team (AI); Claude API; GitHub and Render.com of the project starter.

| Element | Kind | Code / status (proposal, check `software-elements.md`) |
|---|---|---|
| Receive wish | start | `projects.projects` draft (screen 1) |
| Check API key → Key valid? | service task, gateway | `SessionSecrets`; nothing stored on failure |
| Key rejected | end | error shown on screen 1 / 13 |
| Create project | service task | `projects.projects` row |
| Prepare questions → Send questions → Receive answers | sub-process, send, catch | questions_ready → answered |
| Plan requirements and workflow → Send plan for review | sub-process, send | requirements_ready, workflow_drafted |
| Receive confirmed contract → Freeze contract | catch, service task | confirmed; `workflows.contracts` version |
| Prepare build space | service task | `execution.runs` preparing |
| Build each task | call activity (level 2, sequential, once per business task) | runs executing |
| 4 h target → Report overtime warning → Warning shown | non-interrupting timer | run continues; overtime in metrics |
| Run failed | interrupting boundary error | runs failed; report still produced |
| Assemble final report and metrics → Export clean ZIP → Result sent | service tasks, end | runs done; projects done |
| Stop at any time: Receive stop request → Export ZIP of the last checkpoint → Partial result sent | event sub-process | runs stopped |

## Level 2: Build one task (`build-one-task`)

Lanes: Supervisor · Product owner; Architect · Lead · UX/UI; Backend · Frontend; Tester · DevOps; Reviewer · Auditor. Start: Task ready. Ends: Task done, Run failed.

| Activity | Development steps (`workflow.md` 5.2) |
|---|---|
| Plan the task | 1 |
| Define and plan | 2 |
| Design, contract and data (sub-process) | 3, 4, 5 (each only if needed → step_runs not_needed) |
| Build the change | 6 |
| Write tests | 8 |
| Run continuous integration → CI green? | 7 (if needed), 9 |
| Verify as a team, review and audit → Approved? | 10, 11 |
| Fix and verify (on CI red or Approved? No) | 12 (two attempts, then the best model, ADR-038) |
| Not fixable → Ask the client → Continue? | human question (screen 12); task_runs waiting_for_human; No → Run failed |
| Accept | 13 |
| Document | 14 |
| Merge and push | 15 (commit to the starter's public repository; ZIP only without GitHub) |
| Sum up the task | 16 |

Every AI step prompts Claude through Claude Code or opencode with the key of this session (message flows "Prompts" and "Replies").
