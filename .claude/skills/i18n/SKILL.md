---
name: i18n
description: "Internationalisation rules (I18N): locales and language tags, messages, formatting of dates, numbers and money, runtime delivery, the translation resource and the translation workflow (English and German). Use whenever code or a screen shows user-visible text, a date, a number or a locale."
---

# i18n.md: rule summary and index

The full document is [`i18n.md`](i18n.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `I18N` |
| Owner (may change it) | see agents.md 11; others propose a change in the task or pull request (AGT-042) |
| Read first | 4 (messages), 5 (formatting), 7 (the translation resource), 8 (client behaviour). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/i18n/i18n.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/i18n/i18n.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- 1. Scope and definitions
- 2. Constraints and assumptions
- 3. Locales and language tags
- 4. Messages
- 5. Formatting and locale conventions
- 6. Runtime delivery of translations
- 7. The translation resource
- 8. Client behaviour
- 9. Translation workflow
- 10. Content outside interface messages
- 11. Performance of localisation
- 12. Edge cases and error conditions
- 13. Restrictions
- 14. Quality, verification and acceptance
- 15. References
- 16. Project parameters (to be completed by the agent)
