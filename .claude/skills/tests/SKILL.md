---
name: tests
description: "Test rules (TST): test levels L1 to L7, tool selection (JUnit 5, Cucumber-JVM, Testcontainers, ArchUnit, Vitest, Playwright), Gherkin scenarios, test data and fixtures, element selection, visual and accessibility tests, non-functional tests and evidence. Use when writing, running or reviewing tests, or mapping acceptance criteria to tests."
---

# tests.md: rule summary and index

The full document is [`tests.md`](tests.md) in this folder. **It is the source of truth; this file is only the way in.** If the two ever differ, the document wins (AGT-080).

| | |
|---|---|
| Rule IDs | `TST` |
| Owner (may change it) | Tester; others propose a change in the task or pull request (AGT-042) |
| Read first | 3 (levels and the shape of the suite), the section of your level (6 to 11 for L1 to L6), 12 (test data), 13 (selectors), 16 (reporting and evidence). |

## How to use it

1. Do not load the whole document. Find the section or rule you need:
   `grep -n "^## \|^### " .claude/skills/tests/tests.md` (headings) or `grep -n "<RULE-ID>" .claude/skills/tests/tests.md` (one rule).
2. Read only that section with an offset and a limit, then cite the rule ID in the task, finding or commit.
3. A bare name like `backend.md` 4.2 in any document means `.claude/skills/backend/backend.md` section 4.2; `requirements.md`, `engine-architecture.md` and `software-elements.md` are in `ai-docs/`; `agents.md` is at the repository root. The skill `find-rule` resolves any rule ID.
4. If this document contradicts another document or the code, stop and ask (`CLAUDE.md` section 3, AGT-040).

## Sections

- 1. Scope and definitions
- 2. Constraints and assumptions
- 3. Test levels and the shape of the suite
- 4. Selecting the tools
- 5. Behaviour scenarios in Gherkin
- 6. Unit tests (L1)
- 7. Component tests (L2) and the location of frontend unit tests
- 8. Backend integration tests (L3)
- 9. API contract tests (L4)
- 10. Frontend integration tests with a stub server (L5)
- 11. End-to-end tests (L6)
- 12. Test data, fixtures and seeders
- 13. Test identifiers and element selection
- 14. Visual and accessibility tests
- 15. Non-functional tests (L7)
- 16. Reporting and living documentation
- 17. Coverage per change and traceability
- 18. Pipeline, environments and execution
- 19. Stability, flaky tests and isolation
- 20. Test code quality and repository layout
- 21. Deferred items and optional levels
- 22. Edge cases and error conditions
- 23. Restrictions
- 24. Quality, verification and acceptance
- 25. References
- 26. Project parameters (to be completed by the agent)
