-- An optional second builder that runs the independent review steps (D-103). Null harness = no separate
-- reviewer. A run snapshots the reviewer; every agent call records which builder, model and effort ran it.
alter table settings.profile_steps add column review boolean not null default false;
alter table projects.projects add column reviewer_harness text
    check (reviewer_harness in ('claude_code', 'gemini_cli', 'codex_cli'));
alter table projects.projects add column reviewer_model text;
alter table projects.projects add column reviewer_effort text;
alter table execution.runs add column reviewer_harness text
    check (reviewer_harness in ('claude_code', 'gemini_cli', 'codex_cli'));
alter table execution.runs add column reviewer_model text;
alter table execution.runs add column reviewer_effort text;
alter table execution.agent_calls add column harness text;
alter table execution.agent_calls add column model text;
alter table execution.agent_calls add column effort text;
