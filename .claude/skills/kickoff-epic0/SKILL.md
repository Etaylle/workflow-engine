---
name: kickoff-epic0
description: "The first plan-mode session of the project: the Lead checks the Epic 0 plan (goal G-01, due 20 November 2026) and lists the decisions the human takes first. Use when the human types /kickoff-epic0, in plan mode."
disable-model-invocation: true
---

# Kickoff of Epic 0 (plan mode)

Precondition: `node .claude/hooks/selftest.mjs` passed and the session is in **plan mode**. If not in plan mode, say so and stop. Change no file until the human approves.

The prompt is kept in one place: follow **`kickoff-prompt.md` section 3a** at the repository root exactly (it is the same text the human pastes). If the project is already under way (`todo.md` shows a story In progress or Done), follow section 3b instead and say so.

At the end, after approval: update the story cards in `ai-docs/stories/`, the task specs in `ai-docs/tasks/`, the epic file `ai-docs/epics/E-00.md` and `todo.md` (current position, decisions, next).
