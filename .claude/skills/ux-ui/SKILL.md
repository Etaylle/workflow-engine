---
name: ux-ui
description: "UX/UI design system (UX): foundations and tokens, semantic structure, components, patterns, page templates, content and language, accessibility and the edge-case states (empty, loading, error, conflict) of every screen. Use when designing, styling, building or reviewing a screen or one of its states; pair it with the screen images in ai-docs/ui-mockup/."
---

# ux-ui.md: rule summary and index

The full document is [`ux-ui.md`](ux-ui.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `UX` |
| Owner (may change it) | UX/UI Designer; others propose a change in the task or pull request (AGT-042) |
| Read first | 4 (foundations, tokens), 6 and 7 (components, patterns), 8 (page templates), 10 (accessibility), 13 (edge cases and error states). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/ux-ui/ux-ui.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/ux-ui/ux-ui.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- 1. Scope and definitions
- 2. Constraints and assumptions
- 3. Design system architecture and governance
- 4. Foundations
- 5. Semantic structure
- 6. Components
- 7. Patterns
- 8. Page templates
- 9. Content and language
- 10. Accessibility
- 11. Performance
- 12. Consent, privacy and sensitive-data UX
- 13. Edge cases and error conditions
- 14. Restrictions
- 15. Quality, verification and acceptance
- 16. References
- 17. Project parameters (to be completed by the agent)
