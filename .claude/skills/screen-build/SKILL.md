---
name: screen-build
description: "Map one of the 14 MVP screens of the workflow engine (0 All projects … 13 Dialogs and states) to everything needed to build it: mockup image, static page, design source, software-elements section, domain, BPMN elements, requirement figures and rule documents. Use before designing, building, testing or reviewing a screen, a screen element or a domain slice, and when writing a story card that has a screen."
---

# Build a screen: the source-of-truth chain

The screens are the reference for content, order and states (not a pixel spec); `software-elements.md` is the reference for the chain screen element → Svelte component → API operation → backend class → table → AI agent; the BPMN model is the reference for states and transitions. Full context: `ai-docs/README.md` sections 7 (mockup) and 8 (BPMN).

## Steps

1. Find the screen in the table below. **Look at the image** (`Read` the PNG): that is what the client sees.
2. Read only its section of `ai-docs/software-elements.md` (column "Map"): every element there becomes a component, an operation, a class and a table row in the story of its domain. Do not invent elements that are not in the map; an element that is missing is an `OPEN:` point for the Lead (WFL-027).
3. Check the BPMN elements of the screen (column "BPMN") in `ai-docs/README.md` 5 and, for states, `software-elements.md` 6 (status values). End states of the model must stay reachable (`software-elements.md` 7).
4. For exact text, colour and spacing, open the design source `ai-docs/ui-mockup/source/<Source>`; for the click flow, `ai-docs/ui-mockup/pages/<name>.html`.
5. Build it as `CLAUDE.md` 2 and 3 say: every state of the screen (empty, loading, error) and keyboard use; every text from the English and German message files; the API operation in `api/openapi.yaml` first; a test for each behaviour shown.
6. Never show the engine's own configuration file names in the app ("Reset to initial", "recommended"). The demo data on the mockups (canteen pre-order, "Pay", times, costs) is not test data.

## Screens

Image: `ai-docs/ui-mockup/images/<name>.png`. Page: `ai-docs/ui-mockup/pages/<name>.html`. Source: `ai-docs/ui-mockup/source/<Source>`.

| # | Name | Source | Domain | Map (`software-elements.md`) | BPMN (level 1 unless noted) |
|---|---|---|---|---|---|
| 0 | `00-all-projects` | `Projects.dc.html` | projects | 3.0 | Client pool (anyone, no login) |
| 1 | `01-wish-and-ai` | `Main.dc.html` | projects | 3.1 | Receive wish; Check API key; Key valid?; Key rejected; Create project |
| 2 | `02-questions-and-technology` | `Questions.dc.html` | questions | 3.2 | Prepare questions; Send questions; Receive answers |
| 3 | `03-requirements` | `Requirements.dc.html` | documents | 3.3 | Plan requirements and workflow |
| 4 | `04-workflow-bpmn-like` | `Workflow.dc.html` | workflows | 3.4 | Plan requirements and workflow; Send plan for review |
| 5 | `05-edit-a-task` | `StepEdit.dc.html` | workflows | 3.4 | Plan requirements and workflow |
| 6 | `06-ai-team-editable` | `Team.dc.html` | settings | 3.6 | Receive confirmed contract; level 2 lanes (members) |
| 7 | `07-development-workflow-editable` | `DevWorkflow.dc.html` | settings | 3.6 | Receive confirmed contract; level 2 activities (16 steps) |
| 8 | `08-ready-to-run-with-github` | `Ready.dc.html` | workflows, projects | 3.5 | Receive confirmed contract; Freeze contract; GitHub and Render pools |
| 9 | `09-workflow-in-process` | `Running.dc.html` | execution | 3.7 | Prepare build space; Build each task; 4 h target; Stop at any time |
| 10 | `10-run-details` | `RunDetails.dc.html` | execution | 3.7 | Build each task; Assemble final report and metrics; Export clean ZIP |
| 11 | `11-task-details-during-the-run` | `StepRun.dc.html` | execution | 3.7 | Level 2: all activities, CI green?, Approved? |
| 12 | `12-human-action-required` | `StepHelp.dc.html` | execution | 3.7 | Level 2: Not fixable → Ask the client → Continue? |
| 13 | `13-dialogs-and-states` | `States.dc.html` | all | 3.8 | Key rejected; Run failed; Stop at any time |

Requirement figures (`ai-docs/requirements.md` 8.1) `ai-docs/images/mockup-1-wish.png` … `mockup-4-execution.png` are the earlier four-page sketches; the 14 screens above replace them for building.

## Flow between screens

```text
0 → 1 → 2 → 3 → 4 (→ 5) → 6 → 7 → 8 → Run → 9 → 10 / 11 (→ 12)
6 and 7 are also opened from 10 and 11 during a run (read-only contract; changes apply to the next run). 13 = shared dialogs and states.
```
