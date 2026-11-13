---
name: frontend
description: "Frontend rules for SvelteKit, TypeScript and Svelte Flow: structure, semantic domains, the domain contract, components, communication, state, API integration, rendering and loading, errors, accessibility, unit tests, routing and forms. Use when writing, testing or reviewing code in frontend/."
---

# frontend.md: rule summary and index

The full document is [`frontend.md`](frontend.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `section rules` |
| Owner (may change it) | Architect; others propose a change in the task or pull request (AGT-042) |
| Read first | 4 to 6 (structure, domains, domain contract), 7 to 10 (components, state, API), 14 (errors), 17 (unit tests), 19 (prohibited patterns), Annexes A and B (routing, forms). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/frontend/frontend.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/frontend/frontend.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- 1. Authority
- 2. Scope
- 3. Principles
- 4. Structure
- 5. Semantic domains
- 6. Domain contract
- 7. Components
- 8. Communication
- 9. State
- 10. API integration
- 11. Rendering and loading
- 12. UX and styling
- 13. Accessibility
- 14. Errors and resilience
- 15. Security and privacy
- 16. Performance
- 17. Basic component unit tests
- 18. Edge cases
- 19. Prohibited patterns
- 20. Framework profile
- 21. Workflow
- 22. Definition of done
- Annex A (normative): Routing and navigation
- Annex B (normative): Forms
- Annex C (normative): Browser support and progressive enhancement
- Annex D (normative): Dependency and shared-code governance
- Annex E (normative): Readiness and deferred items
- Annex F (informative): Framework profiles
- Annex G (informative): Tooling options
- Annex H (informative): References
