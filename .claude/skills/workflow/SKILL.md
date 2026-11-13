---
name: workflow
description: "Development workflow (WFL) of the workflow engine: goals, epics, user stories, Definition of Ready, the 16 stages of a story, the verification round, Definition of Done, Epic 0 (goal G-01), gates, change management and the templates for story cards, task specs and evidence blocks. Use when planning, starting, verifying, accepting or closing a story or an epic, or writing a story card or task spec."
---

# workflow.md: rule summary and index

The full document is [`workflow.md`](workflow.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `WFL` |
| Owner (may change it) | Lead Developer; others propose a change in the task or pull request (AGT-042) |
| Read first | 4 (stories, DoR 4.2), 5 (work on a story, 5.1 verification round, 5.2 the 16 steps, 5.3 story types), 8 (Definition of Done), 9 (Epic 0), 17 (templates: 17.3 evidence, 17.4 task spec). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/workflow/workflow.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/workflow/workflow.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- Contents and version
- 1. Basis
- 2. Hierarchy and identifiers
- 3. From wishes to goals and epics
- 4. User stories
- 5. Work on a story: every member contributes or checks
- 6. Tests
- 7. The pipeline, the loop and continuous delivery
- 8. Definition of Done (outbound gate)
- 9. Goal G-01: technical project setup (Epic 0)
- 10. Epic structure of the goals
- 11. Epic lifecycle and gates
- 12. Change management
- 13. Dormant phases and the no-touch documents
- 14. Edge cases
- 15. Restrictions
- 16. Parameters
- 17. Templates
- 18. Sources
- 19. Practices taken from the uploaded projects
