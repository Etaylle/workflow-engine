-- Schema of the domain "execution" (M4, thin). Forward-only: a released migration is never edited.
-- Only the tables of D-M3-5 that the first run needs; no foreign key crosses a schema.
CREATE SCHEMA IF NOT EXISTS execution;

-- One run of a confirmed contract. paused = a step failed; interrupted = the engine restarted.
CREATE TABLE execution.runs (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    contract_version integer NOT NULL,
    status text NOT NULL CHECK (status IN ('preparing', 'executing', 'paused', 'done', 'interrupted')),
    failure_reason text,
    cost_limit_usd numeric(12,2) NOT NULL CHECK (cost_limit_usd > 0),
    workspace_path text NOT NULL,
    database_name text,
    last_seq bigint NOT NULL DEFAULT 0,
    started_at timestamptz NOT NULL,
    finished_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX runs_project_idx ON execution.runs (project_id, started_at);

-- One software task of the contract, built after the one before.
CREATE TABLE execution.task_runs (
    id uuid NOT NULL PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES execution.runs (id) ON DELETE CASCADE,
    seq integer NOT NULL,
    title text NOT NULL,
    lane text,
    description text,
    status text NOT NULL CHECK (status IN ('pending', 'running', 'done', 'failed')),
    UNIQUE (run_id, seq)
);

-- One development step of a task (the steps come from the profile, which is data).
CREATE TABLE execution.step_runs (
    id uuid NOT NULL PRIMARY KEY,
    task_run_id uuid NOT NULL REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    seq integer NOT NULL,
    step_key text NOT NULL,
    title text NOT NULL,
    status text NOT NULL CHECK (status IN ('waiting', 'running', 'done', 'failed')),
    result text,
    UNIQUE (task_run_id, seq)
);

-- One Claude Code invocation; the cost limit is the sum of cost_usd of a run.
CREATE TABLE execution.agent_calls (
    id uuid NOT NULL PRIMARY KEY,
    step_run_id uuid NOT NULL REFERENCES execution.step_runs (id) ON DELETE CASCADE,
    attempt integer NOT NULL,
    seconds integer NOT NULL,
    cost_usd numeric(10,4) NOT NULL,
    turns integer NOT NULL,
    outcome text NOT NULL,
    created_at timestamptz NOT NULL
);

-- The git commit in the workspace after a green step.
CREATE TABLE execution.checkpoints (
    id uuid NOT NULL PRIMARY KEY,
    step_run_id uuid NOT NULL REFERENCES execution.step_runs (id) ON DELETE CASCADE,
    commit_sha text NOT NULL,
    created_at timestamptz NOT NULL
);

-- Run events with a number per run: state changes and console lines (replayed to the screen).
CREATE TABLE execution.run_events (
    run_id uuid NOT NULL REFERENCES execution.runs (id) ON DELETE CASCADE,
    seq bigint NOT NULL,
    kind text NOT NULL CHECK (kind IN ('run', 'task', 'step', 'console')),
    text text NOT NULL,
    created_at timestamptz NOT NULL,
    PRIMARY KEY (run_id, seq)
);
