-- How a run builds: the whole software at once, or one task after the other. The project holds the
-- choice (null = automatic, decided by the number of software tasks of the contract), the run a snapshot.
alter table projects.projects add column build_mode text check (build_mode in ('whole', 'tasks'));
alter table execution.runs add column build_mode text not null default 'tasks'
    check (build_mode in ('whole', 'tasks'));
