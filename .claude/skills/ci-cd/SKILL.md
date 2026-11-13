---
name: ci-cd
description: "Pipeline rules (CIC): the ci/run command interface, triggers and branches, steps S01 to S24, layered and single-container images, configuration and secrets, remote deployment, environments and promotion, self-healing, reports and evidence. Use when creating or changing ci/run, a pipeline step, Docker or compose files, or a deployment."
---

# ci-cd.md: rule summary and index

The full document is [`ci-cd.md`](ci-cd.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `CIC` |
| Owner (may change it) | DevOps; others propose a change in the task or pull request (AGT-042) |
| Read first | 4 (repository layout and ci/run, 4.1 the commands), 6 (the steps), 7 (containers), 8 (configuration and secrets), 13 (evidence). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/ci-cd/ci-cd.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/ci-cd/ci-cd.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- Contents and version
- 1. Scope and definitions
- 2. Principles
- 3. Position in the workflow and pipeline levels
- 4. Repository layout and the command interface
- 5. Triggers and branches
- 6. The steps
- 7. Containers
- 8. Configuration and secrets
- 9. Remote deployment
- 10. Environments and promotion
- 11. Providers
- 12. Self-healing and self-learning in the pipeline
- 13. Reports, run record and evidence
- 14. Speed, caching and cost
- 15. Deferred steps (reserved places)
- 16. Quality gates and pipeline change
- 17. Edge cases
- 18. Restrictions
- 19. Parameters
- 20. Templates
- 21. Sources
