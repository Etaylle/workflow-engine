---
name: docs-check
description: "Check that every file reference in the project documents (CLAUDE.md, agents.md, ai-docs/, .claude/) resolves to an existing file, with the script check-refs.mjs. Use after moving, renaming, merging or deleting a document, image, mockup or skill, and before reporting a documentation change."
---

# Check document references

Run from the repository root:

```bash
node .claude/skills/docs-check/check-refs.mjs        # broken references only; exit 1 if any
node .claude/skills/docs-check/check-refs.mjs --all  # also lists references to product code and examples
```

What it checks: Markdown links and backticked paths in every `.md` file outside build folders and `ai-docs/skeleton/`. A path resolves relative to the file, the repository root or `ai-docs/`; a bare document name resolves to any file of that name. Paths into product code that does not exist yet (`backend/`, `frontend/`, `api/`, `ci/` …) and example folder names inside the area documents are counted, not reported.

When it reports a broken reference:

1. Find where the file went: `find ai-docs .claude -name '<name>'`.
2. Fix the reference in the document.
3. A planned document that does not exist yet (`security.md`, a runbook) goes into the `PLANNED` list at the top of the script, with the reason.
4. Run the check again and report the command and its result.
