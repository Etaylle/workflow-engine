---
name: backend
description: "Backend rules (BKD) for Java 21, Spring Boot, Spring AI and Spring Modulith: modular monolith, semantic domains, layers, persistence, Flyway migrations, integration, events, dependable behaviour, configuration, clean code and the Gradle build. Use when writing, testing or reviewing backend code in backend/ or a domain migration."
---

# backend.md: rule summary and index

The full document is [`backend.md`](backend.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `BKD` |
| Owner (may change it) | Architect; others propose a change in the task or pull request (AGT-042) |
| Read first | 3 to 5 (monorepo, domains, layers), the layer you work in (6 to 8), 9 and 10 (persistence, migrations), 13 (dependable behaviour), 15 (clean code). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/backend/backend.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/backend/backend.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- Contents and changes
- 1. Scope and definitions
- 2. Constraints and assumptions
- 3. Architecture and the monorepo
- 4. Semantic domains
- 5. Layers inside a domain
- 6. Domain layer
- 7. Application layer
- 8. Interface layer
- 9. Persistence and database access
- 10. Schema migrations
- 11. Integration with other systems
- 12. Events and messaging
- 13. Dependable behaviour
- 14. Configuration, processes and lifecycle
- 15. Clean code
- 16. Build and dependencies
- 17. Deferred items
- 18. Evolution and other architecture styles
- 19. Framework mapping (informative)
- 20. Edge cases
- 21. Restrictions
- 22. Quality and verification
- 23. References
- 24. Project parameters and records
