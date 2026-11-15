-- Schema of the domain "settings" (M6): the development steps of a task as data, and what a project
-- chose for its runs. Forward-only. No foreign key leaves the schema (project_id is only an id).
CREATE SCHEMA IF NOT EXISTS settings;

-- A step profile: the seeded ones (short, 16 steps) are read-only, a copy is the client's own.
CREATE TABLE settings.step_profiles (
    id uuid NOT NULL PRIMARY KEY,
    key text UNIQUE,
    name text NOT NULL UNIQUE,
    description text NOT NULL DEFAULT '',
    is_default boolean NOT NULL DEFAULT false,
    seeded boolean GENERATED ALWAYS AS (key IS NOT NULL) STORED,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX step_profiles_one_default ON settings.step_profiles (is_default) WHERE is_default;

-- One step of a profile; seq orders them. The prompt may name the task as {task}.
CREATE TABLE settings.profile_steps (
    id uuid NOT NULL PRIMARY KEY,
    profile_id uuid NOT NULL REFERENCES settings.step_profiles (id) ON DELETE CASCADE,
    seq integer NOT NULL CHECK (seq > 0),
    key text NOT NULL,
    title text NOT NULL,
    gated boolean NOT NULL,
    cap_usd numeric(6,2) NOT NULL CHECK (cap_usd > 0 AND cap_usd <= 10),
    prompt text NOT NULL,
    UNIQUE (profile_id, key)
);

CREATE INDEX profile_steps_profile_idx ON settings.profile_steps (profile_id, seq);

-- What a project chose for its runs; a null column means the default.
CREATE TABLE settings.project_settings (
    project_id uuid NOT NULL PRIMARY KEY,
    step_profile_id uuid REFERENCES settings.step_profiles (id) ON DELETE SET NULL,
    cost_limit_usd numeric(12,2) CHECK (cost_limit_usd IS NULL OR cost_limit_usd > 0),
    updated_at timestamptz NOT NULL DEFAULT now()
);
