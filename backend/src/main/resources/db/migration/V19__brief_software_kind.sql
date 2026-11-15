-- The kind of software the wish asks for (console or web), found by the brief step.
ALTER TABLE projects.project_briefs ADD COLUMN software_kind text NOT NULL DEFAULT 'web'
    CHECK (software_kind IN ('console', 'web'));
