-- A step that runs only when the project has a separate reviewer (D-104): the second opinion on the
-- plan. Without a reviewer a run leaves the step out. Only an independent review step can be one.
alter table settings.profile_steps add column reviewer_only boolean not null default false
    check (not reviewer_only or review);
