---
name: architecture
description: "Project architecture entry point of the workflow engine: authority and conflicts between documents, the document map, the decision register ADR-001 to ADR-040, the invariants every agent keeps and the deferred areas. Use when starting any story, checking or citing an ADR, resolving a contradiction between documents or with the code, or before adding a package, folder, domain, dependency or document."
---

# architecture.md: rule summary and index

The full document is [`architecture.md`](architecture.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `ARC, ADR` |
| Owner (may change it) | Architect; others propose a change in the task or pull request (AGT-042) |
| Read first | Sections 2 (authority), 6 (invariants) and 7 (deferred areas) at the start of every session; 5 for a decision; 3 to find which document holds a topic. |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/architecture/architecture.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/architecture/architecture.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- 1. How to use this document
- 2. Authority and conflicts
- 3. The document map
- 4. The architecture at a glance
- 5. The decision register
- 6. Invariants every agent keeps
- 7. Deferred areas
- 8. The document plan
- 9. Decisions and changes
- 10. Start checklist for an agent
