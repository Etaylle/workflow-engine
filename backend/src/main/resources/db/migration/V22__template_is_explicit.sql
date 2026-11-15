-- A project has a template only when the owner chose one; otherwise the kind of the brief decides.
ALTER TABLE projects.projects ALTER COLUMN template_id DROP NOT NULL;
ALTER TABLE projects.projects ALTER COLUMN template_id DROP DEFAULT;
