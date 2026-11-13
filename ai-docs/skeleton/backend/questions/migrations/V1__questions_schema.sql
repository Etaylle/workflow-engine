-- Schema of the domain "questions". Forward-only (db.md 15). Skeleton: keep in step with ai-docs/software-elements.md.
CREATE SCHEMA IF NOT EXISTS questions;

-- One question of the questions agent.
CREATE TABLE questions.questions (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    seq integer NOT NULL,
    category text NOT NULL CHECK (category IN ('requirement', 'feature')),
    answer_type text NOT NULL CHECK (answer_type IN ('single_choice', 'multiple_choice', 'text')),
    text text NOT NULL,
    options jsonb NOT NULL DEFAULT '[]',
    ai_default text,
    explanation text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (project_id, seq)
);

-- The answer of the client, or the AI default when the client lets the AI decide.
CREATE TABLE questions.answers (
    id uuid NOT NULL PRIMARY KEY,
    question_id uuid NOT NULL UNIQUE REFERENCES questions.questions (id) ON DELETE CASCADE,
    value jsonb NOT NULL,
    decided_by text NOT NULL CHECK (decided_by IN ('client', 'ai')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Sources the questions agent used (insert-only).
CREATE TABLE questions.research_sources (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    url text NOT NULL,
    title text,
    summary text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX questions_project_idx ON questions.questions (project_id);
