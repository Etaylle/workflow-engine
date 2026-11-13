---
name: team
description: "Team roster (TEAM) of the AI agent team: roles, responsibilities, member profiles, decision rights, collaboration, team profiles by project size and model assignment. Use when deciding who does, approves, verifies or owns something, or which model a role uses."
---

# team.md: rule summary and index

The full document is [`team.md`](team.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `TEAM` |
| Owner (may change it) | Lead Developer; others propose a change in the task or pull request (AGT-042) |
| Read first | 1 (roster), 2 (your own profile), 3 (decision rights), 5a (model assignment). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/team/team.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/team/team.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- 1. Roster
- 2. Member profiles
- 3. Decision rights
- 4. Collaboration
- 5. Team profiles by project size
- 5a. Model assignment
- 6. Onboarding of a member
- 7. Edge cases
- 8. Roster (fill in per project)
