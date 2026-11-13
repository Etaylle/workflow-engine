---
name: infrastructure
description: "Infrastructure rules (INF): environments local, dev, test and prod, Git branches, promotion and rollback, the local environment, hosts, containers, configuration, data and backups, recovery, the growth path, and the Epic 0 technical setup stories US-00.01 to US-00.13. Use for environment, branch, host, backup or Epic 0 work."
---

# infrastructure.md: rule summary and index

The full document is [`infrastructure.md`](infrastructure.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `INF` |
| Owner (may change it) | DevOps; others propose a change in the task or pull request (AGT-042) |
| Read first | 3 (MVP profile), 4 and 5 (environments, Git), 7 (local environment), 17 (Epic 0 stories). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/infrastructure/infrastructure.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/infrastructure/infrastructure.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- Contents and version
- 1. Scope and definitions
- 2. Principles
- 3. The MVP profile
- 4. Environments
- 5. Git: branches, promotion and rollback
- 6. Repository layout
- 7. The local environment
- 8. Hosts
- 9. Containers
- 10. Configuration and secrets
- 11. Network and entry point
- 12. Data, volumes and backups
- 13. Deployment and recovery
- 14. Dependability
- 15. The growth path
- 16. Deferred areas
- 17. Epic 0: the technical setup stories
- 18. Edge cases
- 19. Restrictions
- 20. Quality gates
- 21. Parameters
- 22. Templates
- 23. Sources and limits
