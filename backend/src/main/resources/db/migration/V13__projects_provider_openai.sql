-- Planning may use an OpenAI-compatible provider besides Anthropic (D-74); builds stay Claude Code.
ALTER TABLE projects.projects DROP CONSTRAINT projects_provider_check;
ALTER TABLE projects.projects
    ADD CONSTRAINT projects_provider_check CHECK (provider IN ('anthropic', 'openai'));
