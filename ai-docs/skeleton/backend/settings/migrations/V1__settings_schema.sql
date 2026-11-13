-- Schema of the domain "settings". Forward-only (db.md 15). Skeleton: keep in step with ai-docs/software-elements.md.
CREATE SCHEMA IF NOT EXISTS settings;

-- An editable team plus development workflow; the default comes from team.md and workflow.md.
CREATE TABLE settings.team_profiles (
    id uuid NOT NULL PRIMARY KEY,
    name text NOT NULL UNIQUE,
    is_default boolean NOT NULL DEFAULT false,
    source text NOT NULL,
    preset text NOT NULL DEFAULT 'custom' CHECK (preset IN ('starter', 'standard', 'full', 'custom')),
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- One member of the AI team (editable).
CREATE TABLE settings.team_members (
    id uuid NOT NULL PRIMARY KEY,
    profile_id uuid NOT NULL REFERENCES settings.team_profiles (id) ON DELETE CASCADE,
    role_key text NOT NULL,
    name text NOT NULL,
    mission text NOT NULL,
    instructions text NOT NULL,
    model_tier text NOT NULL CHECK (model_tier IN ('best', 'standard')),
    active boolean NOT NULL DEFAULT true,
    can_disable boolean NOT NULL DEFAULT true,
    write_paths jsonb NOT NULL DEFAULT '[]',
    seq integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (profile_id, role_key),
    CHECK (active OR can_disable)
);

-- One step every task runs (editable, ordered by seq).
CREATE TABLE settings.dev_workflow_steps (
    id uuid NOT NULL PRIMARY KEY,
    profile_id uuid NOT NULL REFERENCES settings.team_profiles (id) ON DELETE CASCADE,
    seq integer NOT NULL,
    key text NOT NULL,
    name text NOT NULL,
    runs_when text NOT NULL CHECK (runs_when IN ('always', 'if_screen', 'if_interface_changes', 'if_data_changes', 'if_needed', 'after_every_change', 'on_failure')),
    story_types jsonb NOT NULL DEFAULT '["feature", "technical", "defect", "audit"]',
    done_when text,
    time_limit_minutes integer,
    fixed_position text NOT NULL DEFAULT 'none' CHECK (fixed_position IN ('first', 'last', 'none')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (profile_id, key),
    CHECK (time_limit_minutes IS NULL OR time_limit_minutes > 0)
);

-- Which members do or check a step.
CREATE TABLE settings.dev_workflow_step_members (
    step_id uuid NOT NULL REFERENCES settings.dev_workflow_steps (id) ON DELETE CASCADE,
    member_id uuid NOT NULL REFERENCES settings.team_members (id) ON DELETE CASCADE,
    part text NOT NULL CHECK (part IN ('does', 'checks')),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (step_id, member_id)
);

-- Rules for every task: fix loops before asking a human, CI stages, time.
CREATE TABLE settings.workflow_rules (
    profile_id uuid NOT NULL PRIMARY KEY REFERENCES settings.team_profiles (id) ON DELETE CASCADE,
    max_fix_loops integer NOT NULL DEFAULT 2,
    ci_stages jsonb NOT NULL,
    time_target_minutes integer NOT NULL DEFAULT 240,
    ceiling_minutes integer NOT NULL DEFAULT 480,
    raise_model_before_asking boolean NOT NULL DEFAULT true,
    temporary_retries integer NOT NULL DEFAULT 3,
    rebuild_environment_once boolean NOT NULL DEFAULT true,
    set_aside_flaky_tests boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (max_fix_loops BETWEEN 0 AND 5)
);

-- A repeated cause across runs and the automatic check proposed for it (workflow.md 7.5).
CREATE TABLE settings.lessons (
    id uuid NOT NULL PRIMARY KEY,
    profile_id uuid NOT NULL REFERENCES settings.team_profiles (id) ON DELETE CASCADE,
    cause text NOT NULL,
    occurrences integer NOT NULL,
    run_count integer NOT NULL,
    proposal text NOT NULL,
    target_step_key text,
    status text NOT NULL DEFAULT 'proposed' CHECK (status IN ('proposed', 'applied', 'dismissed')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (occurrences >= 2)
);

-- Which model is "best" and "standard" per provider.
CREATE TABLE settings.provider_models (
    id uuid NOT NULL PRIMARY KEY,
    provider text NOT NULL CHECK (provider IN ('anthropic')),
    tier text NOT NULL CHECK (tier IN ('best', 'standard')),
    model_id text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider, tier)
);

