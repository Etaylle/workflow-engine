-- A run keeps the builder it started with, so a resume uses the same one (D-77).
ALTER TABLE execution.runs
    ADD COLUMN harness text NOT NULL DEFAULT 'claude_code' CHECK (harness IN ('claude_code', 'gemini_cli'));

-- Tokens of one builder invocation (Gemini CLI reports tokens, not dollars).
ALTER TABLE execution.agent_calls ADD COLUMN tokens bigint NOT NULL DEFAULT 0;
