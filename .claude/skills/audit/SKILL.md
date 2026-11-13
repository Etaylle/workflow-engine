---
name: audit
description: "Audit method (AUD): audit points AP1 to AP6, the three lenses (technical, tests, business and product) and their checklists, the eight-step method, findings and their lifecycle, cross-domain verification, gate decisions, independence and the report templates. Use for audit work, recording findings, or deciding whether a story, epic, goal or release may pass."
---

# audit.md: rule summary and index

The full document is [`audit.md`](audit.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `AUD` |
| Owner (may change it) | Auditor; others propose a change in the task or pull request (AGT-042) |
| Read first | 4 (audit points), 5 (lenses and checklists), 6 (the method), 7 (findings), 11 (independence and records), 12 (templates). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/audit/audit.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/audit/audit.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- Contents and version
- 1. Scope and definitions
- 2. Principles
- 3. The MVP profile
- 4. Audit points: when an audit happens
- 5. The three lenses and their checklists
- 6. The method of an audit
- 7. Findings: severity, record, lifecycle
- 8. Cross-domain verification
- 9. Decisions and gates
- 10. Patterns and learning
- 11. Independence, access and records
- 12. Reports and templates
- 13. Deferred areas: what the Auditor records
- 14. Edge cases
- 15. Restrictions
- 16. Parameters
- 17. Sources and limits
