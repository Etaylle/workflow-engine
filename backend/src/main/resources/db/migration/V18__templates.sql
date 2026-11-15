-- The template of a project (chosen from the wish, changeable on the review step) and of a run (a snapshot).
ALTER TABLE projects.projects ADD COLUMN template_id text NOT NULL DEFAULT 'sveltekit-sqlite';
ALTER TABLE execution.runs ADD COLUMN template_id text NOT NULL DEFAULT 'java-svelte-postgres';
