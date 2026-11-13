-- Schema of the domain "execution". Forward-only (db.md 15). Skeleton: keep in step with ai-docs/software-elements.md.
CREATE SCHEMA IF NOT EXISTS execution;

-- One execution of a confirmed contract (aggregate root).
CREATE TABLE execution.runs (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    contract_id uuid NOT NULL,
    status text NOT NULL CHECK (status IN ('queued', 'preparing', 'executing', 'paused', 'waiting_for_human', 'resuming', 'done', 'stopped', 'failed')),
    resume_count integer NOT NULL DEFAULT 0,
    cost_used_amount numeric(12,2) NOT NULL DEFAULT 0,
    sandbox_ref text,
    started_at timestamptz,
    ended_at timestamptz,
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (resume_count BETWEEN 0 AND 3)
);

-- One business task in a run (box on the canvas).
CREATE TABLE execution.task_runs (
    id uuid NOT NULL PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES execution.runs (id) ON DELETE CASCADE,
    node_id uuid NOT NULL,
    story_key text,
    seq integer NOT NULL,
    story_type text NOT NULL DEFAULT 'feature' CHECK (story_type IN ('feature', 'technical', 'defect', 'spike', 'audit')),
    status text NOT NULL CHECK (status IN ('pending', 'running', 'fixing', 'waiting_for_human', 'done', 'failed', 'skipped', 'manual')),
    fix_loops integer NOT NULL DEFAULT 0,
    model_raised boolean NOT NULL DEFAULT false,
    summary text,
    started_at timestamptz,
    ended_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (run_id, seq)
);

-- One development workflow step of one task.
CREATE TABLE execution.step_runs (
    id uuid NOT NULL PRIMARY KEY,
    task_run_id uuid NOT NULL REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    seq integer NOT NULL,
    step_key text NOT NULL,
    step_name text NOT NULL,
    member_role_keys jsonb NOT NULL DEFAULT '[]',
    status text NOT NULL CHECK (status IN ('waiting', 'running', 'done', 'not_needed', 'failed', 'skipped')),
    result text,
    started_at timestamptz,
    ended_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (task_run_id, seq)
);

-- One continuous integration run (ci/run all-local).
CREATE TABLE execution.ci_runs (
    id uuid NOT NULL PRIMARY KEY,
    task_run_id uuid NOT NULL REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    number integer NOT NULL,
    status text NOT NULL CHECK (status IN ('running', 'passed', 'failed')),
    stages jsonb NOT NULL,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (task_run_id, number)
);

-- The console of the AI work (insert-only).
CREATE TABLE execution.console_lines (
    id uuid NOT NULL PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES execution.runs (id) ON DELETE CASCADE,
    task_run_id uuid REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    source text NOT NULL,
    level text NOT NULL CHECK (level IN ('info', 'warn', 'error', 'success')),
    message text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- An error, its type, the automatic reaction and how it ended.
CREATE TABLE execution.run_errors (
    id uuid NOT NULL PRIMARY KEY,
    task_run_id uuid NOT NULL REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    step_run_id uuid REFERENCES execution.step_runs (id) ON DELETE CASCADE,
    ci_run_id uuid REFERENCES execution.ci_runs (id) ON DELETE CASCADE,
    error_class text NOT NULL CHECK (error_class IN ('temporary', 'broken_environment', 'flaky_test', 'real_defect', 'requirement_conflict')),
    message text NOT NULL,
    detail text,
    status text NOT NULL CHECK (status IN ('open', 'fixed', 'needs_human', 'skipped')),
    attempts integer NOT NULL DEFAULT 0,
    auto_action text,
    fixed_in_loop integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Human action required: question with short answer options.
CREATE TABLE execution.human_questions (
    id uuid NOT NULL PRIMARY KEY,
    task_run_id uuid NOT NULL REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    run_error_id uuid REFERENCES execution.run_errors (id) ON DELETE CASCADE,
    question text NOT NULL,
    expected text NOT NULL,
    actual text NOT NULL,
    requirement_refs jsonb NOT NULL DEFAULT '[]',
    attempts_summary text NOT NULL,
    cause text NOT NULL,
    options jsonb NOT NULL,
    status text NOT NULL CHECK (status IN ('open', 'answered', 'cancelled')),
    chosen_option text,
    answer_text text,
    answered_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (status <> 'answered' OR answered_at IS NOT NULL)
);

-- A decision the AI took on its own (also DECISIONS.md).
CREATE TABLE execution.decisions (
    id uuid NOT NULL PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES execution.runs (id) ON DELETE CASCADE,
    task_run_id uuid REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    made_by text NOT NULL,
    title text NOT NULL,
    reason text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- A git commit after a green task, and its push to GitHub (step "Merge and push").
CREATE TABLE execution.checkpoints (
    id uuid NOT NULL PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES execution.runs (id) ON DELETE CASCADE,
    task_run_id uuid REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    seq integer NOT NULL,
    commit_sha text NOT NULL,
    push_status text NOT NULL DEFAULT 'not_pushed' CHECK (push_status IN ('not_pushed', 'pushed', 'failed')),
    pushed_at timestamptz,
    commit_url text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (run_id, seq)
);

-- Evidence: which test proves which criterion.
CREATE TABLE execution.criterion_tests (
    id uuid NOT NULL PRIMARY KEY,
    task_run_id uuid NOT NULL REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    criterion_text text NOT NULL,
    test_name text NOT NULL,
    test_level text NOT NULL CHECK (test_level IN ('unit', 'component', 'integration', 'contract', 'frontend_integration', 'end_to_end', 'smoke')),
    result text NOT NULL CHECK (result IN ('waiting', 'running', 'passed', 'failed')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Evidence: verdict of each member in step Team verification.
CREATE TABLE execution.verdicts (
    id uuid NOT NULL PRIMARY KEY,
    task_run_id uuid NOT NULL REFERENCES execution.task_runs (id) ON DELETE CASCADE,
    role_key text NOT NULL,
    verdict text NOT NULL CHECK (verdict IN ('pass', 'fail', 'not_applicable')),
    reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (task_run_id, role_key),
    CHECK (verdict = 'pass' OR reason IS NOT NULL)
);

-- Task spec, report or blocker report of a member (insert-only).
CREATE TABLE execution.member_reports (
    id uuid NOT NULL PRIMARY KEY,
    step_run_id uuid NOT NULL REFERENCES execution.step_runs (id) ON DELETE CASCADE,
    role_key text NOT NULL,
    kind text NOT NULL CHECK (kind IN ('task_spec', 'report', 'blocker')),
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Audit report of a run: three lenses, risks, what was not verified.
CREATE TABLE execution.audit_reports (
    id uuid NOT NULL PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES execution.runs (id) ON DELETE CASCADE,
    is_final boolean NOT NULL DEFAULT false,
    technical text NOT NULL CHECK (technical IN ('pass', 'pass_with_findings', 'fail', 'not_yet')),
    tests text NOT NULL CHECK (tests IN ('pass', 'pass_with_findings', 'fail', 'not_yet')),
    business text NOT NULL CHECK (business IN ('pass', 'pass_with_findings', 'fail', 'not_yet')),
    accepted_risks jsonb NOT NULL DEFAULT '[]',
    not_verified text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- One audit finding.
CREATE TABLE execution.findings (
    id uuid NOT NULL PRIMARY KEY,
    audit_report_id uuid NOT NULL REFERENCES execution.audit_reports (id) ON DELETE CASCADE,
    severity text NOT NULL CHECK (severity IN ('critical', 'major', 'minor', 'observation')),
    lens text NOT NULL CHECK (lens IN ('technical', 'tests', 'business')),
    text text NOT NULL,
    requirement_key text,
    owner_role_key text,
    status text NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'fixed', 'accepted')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Metrics of a run (workflow.md 7.9).
CREATE TABLE execution.run_metrics (
    run_id uuid NOT NULL PRIMARY KEY REFERENCES execution.runs (id) ON DELETE CASCADE,
    loops_per_task numeric(6,2) NOT NULL DEFAULT 0,
    first_try_tasks integer NOT NULL DEFAULT 0,
    finished_tasks integer NOT NULL DEFAULT 0,
    avg_task_minutes integer NOT NULL DEFAULT 0,
    waiting_minutes integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (first_try_tasks <= finished_tasks)
);

-- One AI call (no prompts, no keys).
CREATE TABLE execution.agent_calls (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    run_id uuid REFERENCES execution.runs (id) ON DELETE CASCADE,
    agent text NOT NULL,
    model text NOT NULL,
    input_tokens integer NOT NULL DEFAULT 0,
    output_tokens integer NOT NULL DEFAULT 0,
    duration_ms integer NOT NULL DEFAULT 0,
    status text NOT NULL CHECK (status IN ('ok', 'invalid_output', 'error')),
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Durable job table of the orchestrator (ADR-018).
CREATE TABLE execution.jobs (
    id uuid NOT NULL PRIMARY KEY,
    kind text NOT NULL,
    payload jsonb NOT NULL,
    status text NOT NULL CHECK (status IN ('pending', 'processing', 'done', 'failed')),
    attempts integer NOT NULL DEFAULT 0,
    available_at timestamptz NOT NULL,
    lease_until timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX console_lines_run_created_idx ON execution.console_lines (run_id, created_at);
CREATE INDEX jobs_pending_idx ON execution.jobs (available_at) WHERE status = 'pending';
CREATE INDEX task_runs_run_idx ON execution.task_runs (run_id);
