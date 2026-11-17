-- Budgets are optional and off by default (decision D-105): a run without a cost limit has none, a
-- step without a cap has none. The caps that existed are cleared; a client sets one where wanted.
alter table execution.runs alter column cost_limit_usd drop not null;

alter table settings.profile_steps alter column cap_usd drop not null;
alter table settings.profile_steps drop constraint profile_steps_cap_usd_check;
alter table settings.profile_steps add constraint profile_steps_cap_usd_check
    check (cap_usd is null or cap_usd > 0);
update settings.profile_steps set cap_usd = null;

-- The effort of one step (D-106); null means the builder choice of the project.
alter table settings.profile_steps add column effort text
    check (effort is null or effort in ('low', 'medium', 'high', 'xhigh', 'max'));
