---
name: db
description: "Database rules (DBS) for PostgreSQL: one schema per domain, modelling, naming, types, keys and identifiers, constraints, standard columns, statuses, indexes, transactions, forward-only migrations and seeds that give the same result when run twice. Use when changing a schema, a migration, a query or a seed."
---

# db.md: rule summary and index

The full document is [`db.md`](db.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `DBS` |
| Owner (may change it) | Architect; others propose a change in the task or pull request (AGT-042) |
| Read first | 2 (MVP profile), 3 (one schema per domain), 5 to 10 (naming, types, keys, constraints, statuses), 15 (migrations), 16 (seeds). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/db/db.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/db/db.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- Contents and version
- 1. Scope, definitions, rule classes and sources
- 2. The MVP profile
- 3. Architecture: one engine, one schema per domain
- 4. Modelling method
- 5. Naming and documentation
- 6. Types
- 7. Keys and identifiers
- 8. Relations and constraints
- 9. Standard columns, concurrency and deletion
- 10. Statuses and vocabularies
- 11. Special data
- 12. Indexes
- 13. Queries, views, functions and triggers
- 14. Transactions, concurrency and side effects
- 15. Schema migrations
- 16. Data sets and seeds
- 17. Access accounts, backup and recovery
- 18. Importing and migrating existing data
- 19. Performance and capacity
- 20. Testing the database
- 21. Tools and engines
- 22. Documentation of the schema
- 23. Deferred: security, observability and scale
- 24. Edge cases
- 25. Restrictions
- 26. Quality
- 27. Antipatterns
- 28. Sources and limits
- 29. Parameters and registers
