---
name: find-rule
description: "Resolve a rule ID or a bare document reference of the workflow engine (WFL-050, BKD-MIG-009, ADR-039, E-ADR-12, FR-AI-06, AGT-042, `backend.md` 4.2 …) to its file and read only that rule or section. Use whenever a task, finding, document or commit cites a rule ID or a document by bare name."
---

# Find a rule

| Prefix | File |
|---|---|
| `ARC`, `ADR` | `.claude/skills/architecture/architecture.md` (decision register: section 5) |
| `WFL` | `.claude/skills/workflow/workflow.md` |
| `TEAM` | `.claude/skills/team/team.md` |
| `AGT` | `agents.md` (repository root) |
| `UX` | `.claude/skills/ux-ui/ux-ui.md` |
| frontend section rules | `.claude/skills/frontend/frontend.md` |
| `API` | `.claude/skills/api/api.md` |
| `BKD` | `.claude/skills/backend/backend.md` |
| `DBS` | `.claude/skills/db/db.md` |
| `I18N` | `.claude/skills/i18n/i18n.md` |
| `TST` | `.claude/skills/tests/tests.md` |
| `CIC` | `.claude/skills/ci-cd/ci-cd.md` |
| `INF` | `.claude/skills/infrastructure/infrastructure.md` |
| `AUD` | `.claude/skills/audit/audit.md` |
| `E-ADR` | `ai-docs/engine-architecture.md` |
| `BR`, `BRU`, `UR`, `FR` (incl. `FR-AGT`), `QR`, `TR`, `IR`, `OI` | `ai-docs/requirements.md` |
| `US-ee.ss` | `ai-docs/stories/US-ee.ss.md`; Epic 0 stories also in `infrastructure.md` 17 |

Bare names: `requirements.md`, `engine-architecture.md`, `software-elements.md` → `ai-docs/`; `agents.md` → root; any other `<area>.md` → `.claude/skills/<area>/<area>.md`. Screens → skill `screen-build`; BPMN → skill `bpmn-model`.

## Steps

1. `grep -n "| <ID> |" <file>` finds the defining row of a rule (rules are table rows `| ID | text |`). If nothing matches, `grep -rn "<ID>" CLAUDE.md agents.md ai-docs .claude/skills` shows every use.
2. For a section number (`workflow.md` 5.2): `grep -n "^#\+ 5.2" <file>`, then read from that line with a limit.
3. A rule ID that is cited but defined nowhere is a broken reference: report it (it is a finding for the document owner), do not guess its meaning.
