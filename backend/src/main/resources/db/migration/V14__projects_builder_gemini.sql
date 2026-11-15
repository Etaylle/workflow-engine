-- The builder of a project is Claude Code or Gemini CLI (D-77); the earlier value opencode is gone.
UPDATE projects.projects SET agent_harness = 'claude_code' WHERE agent_harness = 'opencode';
ALTER TABLE projects.projects DROP CONSTRAINT projects_agent_harness_check;
ALTER TABLE projects.projects
    ADD CONSTRAINT projects_agent_harness_check CHECK (agent_harness IN ('claude_code', 'gemini_cli'));
