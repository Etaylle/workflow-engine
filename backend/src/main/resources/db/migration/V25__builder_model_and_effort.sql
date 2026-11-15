-- The model and the effort of the builder: the project holds the choice (null = the engine's default),
-- the run a snapshot (the model is the one really used, the effort null = the CLI's own default).
alter table projects.projects add column builder_model text;
alter table projects.projects add column builder_effort text;
alter table execution.runs add column builder_model text;
alter table execution.runs add column builder_effort text;
