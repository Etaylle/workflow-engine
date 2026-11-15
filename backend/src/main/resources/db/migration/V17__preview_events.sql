-- The preview of the generated app writes its console output (app) and its state changes (preview)
-- into the event stream of the run.
ALTER TABLE execution.run_events DROP CONSTRAINT run_events_kind_check;
ALTER TABLE execution.run_events ADD CONSTRAINT run_events_kind_check
    CHECK (kind IN ('run', 'task', 'step', 'console', 'app', 'preview'));
