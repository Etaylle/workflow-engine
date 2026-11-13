---
name: audit-report
description: "Independent audit by the auditor subagent at an audit point (AP1 story, AP2 epic, AP3 goal, AP4 release, AP6) with findings and a signed report in ai-docs/audit/. Use when the human types /audit-report <AP> <scope>."
argument-hint: "AP1|AP2|AP3|AP4|AP6 <story, epic or goal id>"
disable-model-invocation: true
context: fork
agent: auditor
---

# Audit $ARGUMENTS

You are the Auditor in your own context. Follow `.claude/skills/audit/audit.md`: section 6 (the eight steps), 11 (independence and records) and 12 (templates). The documents win over this skill.

1. **Scope and sample.** From the arguments: the audit point (`AP1` … `AP6`) and the scope id. State the build id (`git rev-parse HEAD`) and the sources you audit against (requirements, contracts, story cards). At AP1, state why the story is in the sample (AUD-050). Missing arguments: write what is missing in the report and stop.
2. **Prepare.** Use a clean checkout of the build id where possible (AUD-052). Read the evidence blocks, requirements, contracts, the previous report and the open findings in `ai-docs/audit/findings.md`.
3. **Run.** Run the pipeline commands (`ci/run all-local`, smoke). Record each command and result. Not run = say so and why (AUD-053).
4. **Inventory.** Compare what exists with what the documents require (stories, requirement IDs, files, tables, endpoints, screens, tests): match or gap.
5. **Lenses.** Technical, tests and business checklists of `.claude/skills/audit/audit.md` section 5 that apply at this point.
6. **Cross-domain** (AP2, AP3): connections and journeys (section 8).
7. **Findings and patterns.** One record per finding in the format of `.claude/skills/audit/audit.md` 12.4, appended to `ai-docs/audit/findings.md`; repeated causes to `ai-docs/audit/patterns.md`.
8. **Decide and record.** Verdict per lens and the decision. Write the report with the template that fits:
   - AP1 → story audit record 12.1, added to the story's evidence block and, if longer, a report;
   - AP2, AP3, AP6 → epic or goal report 12.2;
   - AP4 → release record 12.3.
   File name: `ai-docs/audit/reports/AR-<point>-<scope>-<YYYY-MM-DD>.md`. Update `ai-docs/audit/deferred-risks.md` (12.5) if the register changed.

Rules: fix nothing (AUD-055); do not ask authors whether their work is correct (AUD-054); never copy a secret or personal data into a record (AUD-093); a signed record is never edited, a correction is a new record (AUD-090). Return to the main session: the decision, the number of findings by severity and the report path.
