-- US-06.06: a run waits (status waiting) instead of pausing; the wait has a kind. A defect that
-- is not fixable after the fix attempts becomes one question for the client.
ALTER TABLE execution.runs DROP CONSTRAINT runs_status_check;
UPDATE execution.runs SET status = 'interrupted' WHERE status = 'paused';
ALTER TABLE execution.runs ADD CONSTRAINT runs_status_check
    CHECK (status IN ('preparing', 'executing', 'waiting', 'done', 'interrupted'));
ALTER TABLE execution.runs ADD COLUMN wait_kind text
    CHECK (wait_kind IN ('question', 'cost_limit', 'tokens'));

CREATE TABLE execution.human_questions (
    id uuid NOT NULL PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES execution.runs (id) ON DELETE CASCADE,
    step_run_id uuid NOT NULL REFERENCES execution.step_runs (id) ON DELETE CASCADE,
    question text NOT NULL,
    asked_at timestamptz NOT NULL,
    choice text CHECK (choice IN ('retry', 'skip', 'stop')),
    hint text,
    answered_at timestamptz
);

CREATE INDEX human_questions_run_idx ON execution.human_questions (run_id, asked_at);
