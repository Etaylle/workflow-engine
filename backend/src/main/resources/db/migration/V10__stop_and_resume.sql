-- US-06.09 / 06.10: the client's stop ends a run as failed (the only way to failed); a run
-- interrupted by an engine restart can be resumed a bounded number of times.
ALTER TABLE execution.runs DROP CONSTRAINT runs_status_check;
ALTER TABLE execution.runs ADD CONSTRAINT runs_status_check
    CHECK (status IN ('preparing', 'executing', 'waiting', 'done', 'interrupted', 'failed'));
ALTER TABLE execution.runs ADD COLUMN resume_count integer NOT NULL DEFAULT 0;
