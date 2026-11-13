---
name: agent-rules
description: "Agent rules (AGT) of the workflow engine team in agents.md at the repository root: scope per role, permissions and write areas, context and start order, guardrails, stop rules, run discipline, checkpoints, reports, model assignment and the project inventory. Use when starting as an agent, before writing outside your area, when blocked, when writing a report or blocker report, or when mapping a rule to Claude Code (subagents, hooks, skills)."
---

# agents.md: rule summary and index

The full document is [`agents.md`](../../../agents.md) at the repository root (one copy only). **It is the source of truth; this file is only the way in.** Owner: Lead Developer.

| Read first | Section |
|---|---|
| Roles and what each may not do | 2 |
| Stop and ask (AGT-040), owners of documents (AGT-042) | 5 |
| Project inventory: stack, folders, product references | 8 |
| Write areas per role (enforced by `.claude/hooks/guard.mjs`) | 11 |
| Run discipline: context packet, checkpoints (AGT-054, AGT-067, AGT-068) | 12 |
| Running the team in Claude Code: subagents, hooks, skills (AGT-080 to AGT-084) | 13 |
| Restrictions | 14 |
| Report and blocker report formats | 15.2, 15.3 |

Find a rule: `grep -n "| AGT-0NN |" agents.md`; headings: `grep -n "^## \|^### " agents.md`. The file is read-only on disk on purpose: propose changes to the Lead.
