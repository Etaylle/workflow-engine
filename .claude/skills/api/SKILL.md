---
name: api
description: "API contract rules (API) for the contract-first OpenAPI file api/openapi.yaml: resources and URLs, HTTP semantics, representations, errors, collections, writes with idempotency and concurrency, caching, versioning, the interactive reference and quality gates. Use when defining or changing an API operation, generating clients or servers from the contract, or reviewing a controller or frontend call against it."
---

# api.md: rule summary and index

The full document is [`api.md`](api.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `API` |
| Owner (may change it) | Architect; others propose a change in the task or pull request (AGT-042) |
| Read first | 3 (the contract document), 5 to 10 (resources, HTTP, representations, errors, collections, writes), 12 (evolution), 14 (quality gates). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/api/api.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/api/api.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- 1. Scope and definitions
- 2. Constraints and assumptions
- 3. The contract document
- 4. Generation and consumer adaptation
- 5. Resources and URLs
- 6. HTTP semantics
- 7. Representations
- 8. Errors
- 9. Collections
- 10. Writes, idempotency and concurrency
- 11. Caching and conditional requests
- 12. Evolution, versioning and deprecation
- 13. Documentation and the interactive reference
- 14. Quality gates
- 15. Operational aspects and deferred items
- 16. Edge cases and error conditions
- 17. Restrictions
- 18. Quality, verification and acceptance
- 19. References
- 20. Project parameters (to be completed by the agent)
