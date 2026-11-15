-- The third builder, Codex CLI (D-102), for projects and for the runs that snapshot it.
ALTER TABLE projects.projects DROP CONSTRAINT projects_agent_harness_check;
ALTER TABLE projects.projects ADD CONSTRAINT projects_agent_harness_check
    CHECK (agent_harness IN ('claude_code', 'gemini_cli', 'codex_cli'));
ALTER TABLE execution.runs DROP CONSTRAINT runs_harness_check;
ALTER TABLE execution.runs ADD CONSTRAINT runs_harness_check
    CHECK (harness IN ('claude_code', 'gemini_cli', 'codex_cli'));
